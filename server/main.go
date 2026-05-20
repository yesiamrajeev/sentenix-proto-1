package main

import (
	"crypto/rand"
	"encoding/hex"
	"encoding/json"
	"fmt"
	"net/http"
	"os"
	"runtime"
	"strconv"
	"strings"
	"sync/atomic"
	"time"
)

const (
	serviceName    = "app.config-svc"
	serviceVersion = "v0.1.0"
	gitSha         = "a3f9c12"
	listenAddr     = ":8080"
	configPath     = "/app/config.xml"
)

var (
	requestSeq   uint64
	useColor     = os.Getenv("LOG_COLOR") != "false"
	configXMLRaw []byte
)

const (
	ansiReset  = "\033[0m"
	ansiGreen  = "\033[32m"
	ansiCyan   = "\033[36m"
	ansiYellow = "\033[33m"
	ansiRed    = "\033[31m"
	ansiGray   = "\033[90m"
	ansiBold   = "\033[1m"
)

func color(code, s string) string {
	if !useColor {
		return s
	}
	return code + s + ansiReset
}

func levelColor(level string) string {
	switch level {
	case "INFO":
		return ansiGreen
	case "DEBUG":
		return ansiCyan
	case "WARN":
		return ansiYellow
	case "ERROR":
		return ansiRed
	}
	return ""
}

func nowTS() string {
	return time.Now().UTC().Format("2006-01-02T15:04:05.000Z")
}

func randHex(n int) string {
	b := make([]byte, n)
	_, _ = rand.Read(b)
	return hex.EncodeToString(b)
}

func newUUID() string {
	b := make([]byte, 16)
	_, _ = rand.Read(b)
	b[6] = (b[6] & 0x0f) | 0x40
	b[8] = (b[8] & 0x3f) | 0x80
	return fmt.Sprintf("%x-%x-%x-%x-%x", b[0:4], b[4:6], b[6:8], b[8:10], b[10:])
}

// logLine emits one structured log line.
// Format: <ts> <LEVEL> <component>[<tag>]  <msg> k=v k=v ...
func logLine(level, component, tag, msg string, fields ...string) {
	ts := color(ansiGray, nowTS())
	lvl := color(levelColor(level), fmt.Sprintf("%-5s", level))
	comp := color(ansiBold, fmt.Sprintf("%s[%s]", component, tag))
	pad := strings.Repeat(" ", max(0, 38-len(component)-len(tag)-2))

	var b strings.Builder
	b.WriteString(ts)
	b.WriteByte(' ')
	b.WriteString(lvl)
	b.WriteByte(' ')
	b.WriteString(comp)
	b.WriteString(pad)
	b.WriteByte(' ')
	b.WriteString(msg)
	for i := 0; i+1 < len(fields); i += 2 {
		b.WriteByte(' ')
		b.WriteString(color(ansiCyan, fields[i]))
		b.WriteByte('=')
		b.WriteString(fields[i+1])
	}
	fmt.Fprintln(os.Stdout, b.String())
}

func max(a, b int) int {
	if a > b {
		return a
	}
	return b
}

type checkUpdateResponse struct {
	Version int    `json:"version"`
	XML     string `json:"xml"`
}

func handleCheckUpdate(w http.ResponseWriter, r *http.Request) {
	start := time.Now()
	atomic.AddUint64(&requestSeq, 1)

	remote := r.RemoteAddr
	xReqID := r.Header.Get("X-Request-Id")
	if xReqID == "" {
		xReqID = newUUID()
	}
	traceID := randHex(16)
	spanID := randHex(8)
	role := r.Header.Get("X-Client-Role")
	userAgent := r.Header.Get("User-Agent")
	if userAgent == "" {
		userAgent = "-"
	}
	accept := r.Header.Get("Accept")
	if accept == "" {
		accept = "*/*"
	}

	logLine("INFO", "envoy.http", "main",
		"downstream connection accepted",
		"remote", remote,
		"x-request-id", xReqID,
	)
	logLine("INFO", "istio.proxy", "inbound",
		"request entering via istio gateway",
		"gateway", "ingressgateway",
		"listener", "0.0.0.0:8080",
		"cluster", "inbound|8080||hershield-config.default.svc.cluster.local",
	)
	logLine("INFO", "istio.mixer", "authn",
		"mTLS handshake verified",
		"peer", "spiffe://cluster.local/ns/default/sa/default",
		"cipher", "TLS_AES_256_GCM_SHA384",
	)
	logLine("INFO", serviceName, "req",
		fmt.Sprintf("%s %s %s", r.Method, r.URL.Path, r.Proto),
		"trace_id", traceID,
		"span_id", spanID,
	)
	logLine("DEBUG", serviceName, "headers",
		"request headers",
		"User-Agent", strconv.Quote(userAgent),
		"X-Client-Role", strconv.Quote(role),
		"Accept", strconv.Quote(accept),
	)

	// Authorization check
	decision := "DENY"
	if role == "admin" {
		decision = "ALLOW"
	}
	logLine("INFO", serviceName, "authz",
		"role check",
		"header", "X-Client-Role",
		"value", strconv.Quote(role),
		"decision", decision,
		"policy", "admin-only.v1",
	)

	if decision != "ALLOW" {
		w.WriteHeader(http.StatusNoContent)
		dur := time.Since(start)
		logLine("INFO", "envoy.http", "main",
			"response sent",
			"status", "204",
			"duration_ms", fmt.Sprintf("%d", dur.Milliseconds()),
			"upstream_service_time", fmt.Sprintf("%d", dur.Milliseconds()),
		)
		logLine("INFO", "istio.proxy", "outbound",
			"egress recorded",
			"bytes_sent", "0",
			"bytes_received", "0",
		)
		fmt.Println()
		return
	}

	logLine("INFO", serviceName, "config",
		"loading config.xml",
		"path", configPath,
		"version", "2",
		"size", fmt.Sprintf("%dB", len(configXMLRaw)),
	)

	payload := checkUpdateResponse{
		Version: 2,
		XML:     string(configXMLRaw),
	}
	body, err := json.Marshal(payload)
	if err != nil {
		logLine("ERROR", serviceName, "encode",
			"failed to serialize response",
			"error", strconv.Quote(err.Error()),
		)
		http.Error(w, "internal error", http.StatusInternalServerError)
		return
	}

	logLine("INFO", serviceName, "encode",
		"serialized response",
		"content-type", "application/json",
		"bytes", fmt.Sprintf("%d", len(body)),
	)

	w.Header().Set("Content-Type", "application/json")
	w.Header().Set("X-Request-Id", xReqID)
	w.Header().Set("X-Trace-Id", traceID)
	w.WriteHeader(http.StatusOK)
	_, _ = w.Write(body)

	dur := time.Since(start)
	logLine("INFO", "envoy.http", "main",
		"response sent",
		"status", "200",
		"duration_ms", fmt.Sprintf("%d", dur.Milliseconds()),
		"upstream_service_time", fmt.Sprintf("%d", dur.Milliseconds()),
	)
	logLine("INFO", "istio.proxy", "outbound",
		"egress recorded",
		"bytes_sent", fmt.Sprintf("%d", len(body)),
		"bytes_received", "0",
	)
	fmt.Println()
}

func startupBanner() {
	hr := strings.Repeat("=", 78)
	banner := []string{
		hr,
		fmt.Sprintf(" %s  service=%s  version=%s  git=%s", color(ansiBold, "hershield-config-svc"), serviceName, serviceVersion, gitSha),
		fmt.Sprintf(" listen=%s  pid=%d  go=%s  goroutines=%d", listenAddr, os.Getpid(), runtime.Version(), runtime.NumGoroutine()),
		hr,
	}
	for _, line := range banner {
		fmt.Fprintln(os.Stdout, color(ansiGreen, line))
	}
	logLine("INFO", "envoy.proxy", "init", "envoy sidecar attached (simulated)",
		"build", "envoy-1.29.4/Modified/RELEASE",
		"mode", "Sidecar",
	)
	logLine("INFO", "istio.pilot", "xds", "received CDS/EDS/LDS/RDS configuration from istiod",
		"node", "sidecar~10.0.0.42~hershield-config-7d4f9b6c5f-x8q2z.default~default.svc.cluster.local",
	)
	logLine("INFO", serviceName, "boot", "configuration loaded",
		"config", configPath,
		"bytes", fmt.Sprintf("%d", len(configXMLRaw)),
	)
	logLine("INFO", serviceName, "boot", "http server listening",
		"addr", listenAddr,
		"handler", "/check-update",
	)
}

func heartbeat() {
	tick := time.NewTicker(30 * time.Second)
	defer tick.Stop()
	for range tick.C {
		var m runtime.MemStats
		runtime.ReadMemStats(&m)
		logLine("DEBUG", serviceName, "metrics",
			"heartbeat",
			"goroutines", fmt.Sprintf("%d", runtime.NumGoroutine()),
			"heap_alloc", fmt.Sprintf("%dKB", m.HeapAlloc/1024),
			"gc_cycles", fmt.Sprintf("%d", m.NumGC),
			"requests_total", fmt.Sprintf("%d", atomic.LoadUint64(&requestSeq)),
		)
	}
}

func loadConfig() error {
	// Try the container path first, then fall back to local-dev path.
	candidates := []string{configPath, "./config.xml", "server/config.xml"}
	for _, p := range candidates {
		b, err := os.ReadFile(p)
		if err == nil {
			configXMLRaw = b
			return nil
		}
	}
	return fmt.Errorf("config.xml not found in any of: %v", candidates)
}

func main() {
	if err := loadConfig(); err != nil {
		fmt.Fprintln(os.Stderr, "FATAL: "+err.Error())
		os.Exit(1)
	}

	startupBanner()
	go heartbeat()

	mux := http.NewServeMux()
	mux.HandleFunc("/check-update", handleCheckUpdate)
	mux.HandleFunc("/healthz", func(w http.ResponseWriter, r *http.Request) {
		w.WriteHeader(http.StatusOK)
		_, _ = w.Write([]byte("ok"))
	})

	srv := &http.Server{
		Addr:              listenAddr,
		Handler:           mux,
		ReadHeaderTimeout: 5 * time.Second,
	}
	if err := srv.ListenAndServe(); err != nil {
		logLine("ERROR", serviceName, "fatal", "http server exited", "error", strconv.Quote(err.Error()))
		os.Exit(1)
	}
}
