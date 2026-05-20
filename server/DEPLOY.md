# HerShield Config Service — AWS EC2 Deployment Guide

End-to-end guide to deploy the Go config server on an AWS EC2 Ubuntu 22.04 instance, dockerized, listening on port 8080 over plain HTTP.

---

## 0. Prerequisites

- An AWS account with permission to create EC2 instances, security groups, and key pairs.
- A local terminal with `ssh` and `scp`.
- This repository checked out locally (you need the `server/` folder).

---

## 1. Launch an EC2 instance

**AWS Console → EC2 → Instances → Launch instances**

1. **Name**: `hershield-config-svc`
2. **AMI**: *Ubuntu Server 22.04 LTS (HVM), SSD Volume Type — 64-bit (Arm)* if you want free-tier with `t4g.micro`, **or** *64-bit (x86)* for `t2.micro`. (Both are free-tier eligible. Pick **x86 / t2.micro** if unsure — the Docker image you build later must match the architecture.)
3. **Instance type**: `t2.micro` (free-tier).
4. **Key pair**: create a new one named `hershield-key`, download the `.pem` file (e.g. `~/Downloads/hershield-key.pem`), set permissions:
   ```bash
   chmod 400 ~/Downloads/hershield-key.pem
   ```
5. **Network settings → Edit**:
   - VPC: default
   - Auto-assign public IP: **Enable**
   - **Firewall (security groups)**: *Create security group*
     - Name: `hershield-config-sg`
     - Inbound rules (add all three):

       | Type        | Protocol | Port | Source              | Purpose            |
       |-------------|----------|------|---------------------|--------------------|
       | SSH         | TCP      | 22   | My IP               | Admin shell access |
       | Custom TCP  | TCP      | 8080 | 0.0.0.0/0           | App traffic        |
       | Custom TCP  | TCP      | 8080 | ::/0                | App traffic (IPv6) |

6. **Storage**: leave default (8 GB gp3).
7. **Launch instance**.

Once `Running`, copy the **Public IPv4 address** — call it `EC2_IP`.

---

## 2. SSH into the instance

```bash
ssh -i ~/Downloads/hershield-key.pem ubuntu@<EC2_IP>
```

If you see `WARNING: UNPROTECTED PRIVATE KEY FILE!`, re-run `chmod 400` on the `.pem`.

---

## 3. Install Docker on the instance

Inside the SSH session:

```bash
sudo apt-get update
sudo apt-get install -y ca-certificates curl gnupg
sudo install -m 0755 -d /etc/apt/keyrings
curl -fsSL https://download.docker.com/linux/ubuntu/gpg | sudo gpg --dearmor -o /etc/apt/keyrings/docker.gpg
sudo chmod a+r /etc/apt/keyrings/docker.gpg
echo \
  "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.gpg] https://download.docker.com/linux/ubuntu \
  $(. /etc/os-release && echo "$VERSION_CODENAME") stable" | \
  sudo tee /etc/apt/sources.list.d/docker.list > /dev/null
sudo apt-get update
sudo apt-get install -y docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin
sudo usermod -aG docker ubuntu
exit
```

Reconnect (the group change takes effect on a new shell):

```bash
ssh -i ~/Downloads/hershield-key.pem ubuntu@<EC2_IP>
docker --version   # should print e.g. Docker version 27.x.x
```

---

## 4. Copy the server source to the instance

From your **local machine** (in the repo root):

```bash
scp -i ~/Downloads/hershield-key.pem -r server ubuntu@<EC2_IP>:~/server
```

This copies `main.go`, `go.mod`, `config.xml`, and `Dockerfile` into `/home/ubuntu/server/`.

---

## 5. Build the Docker image on the instance

Back inside the SSH session:

```bash
cd ~/server
docker build -t hershield-config-svc:latest .
```

First build takes ~1–2 minutes (downloads `golang:1.22-alpine`).

---

## 6. Run the container

```bash
docker run -d \
  --name hershield-config-svc \
  --restart unless-stopped \
  -p 8080:8080 \
  -e LOG_COLOR=false \
  hershield-config-svc:latest
```

> `LOG_COLOR=false` is set because Docker's default detached log capture is not a TTY; ANSI codes would appear as garbage. Switch to `true` if you `docker run -it` interactively for a demo.

Check it's up:

```bash
docker ps
docker logs hershield-config-svc
```

You should see the startup banner with `envoy sidecar attached (simulated)` and `http server listening`.

---

## 7. Smoke test from your laptop

```bash
# Should return 204
curl -i -H "X-Client-Role: user" http://<EC2_IP>:8080/check-update

# Should return 200 + JSON
curl -i -H "X-Client-Role: admin" http://<EC2_IP>:8080/check-update
```

If the call times out, the security group inbound rule for port 8080 is not in place — re-check step 1.5.

---

## 8. Live-tail the server logs during your demo

```bash
docker logs -f hershield-config-svc
```

To see colors during the live demo, run the container in the foreground with TTY instead of detached:

```bash
docker stop hershield-config-svc && docker rm hershield-config-svc
docker run --rm -it \
  --name hershield-config-svc \
  -p 8080:8080 \
  -e LOG_COLOR=true \
  hershield-config-svc:latest
```

---

## 9. Point the Android app at the EC2 IP

Edit `app/src/main/java/com/example/sentenix_proto_1/update/UpdateClient.java`:

```java
public static final String ENDPOINT = "http://<EC2_IP>:8080/check-update";
```

Rebuild and reinstall the app. The toggle + Check-for-update flow will hit the EC2 server.

---

## 10. Updating the config without rebuilding the image

If you change `config.xml` and want the live server to pick it up:

```bash
# On the instance, copy the new file into the running container
scp -i ~/Downloads/hershield-key.pem server/config.xml ubuntu@<EC2_IP>:~/server/config.xml
ssh -i ~/Downloads/hershield-key.pem ubuntu@<EC2_IP>
docker cp ~/server/config.xml hershield-config-svc:/app/config.xml
docker restart hershield-config-svc
```

The server loads `config.xml` once at startup, so a restart is required.

---

## 11. Tear down

```bash
docker stop hershield-config-svc && docker rm hershield-config-svc
```

To delete the instance entirely: **EC2 Console → Instances → select instance → Instance state → Terminate instance**. Don't forget to also delete the security group and key pair if you no longer need them.

---

## Troubleshooting

| Symptom | Likely cause | Fix |
|---|---|---|
| `connection refused` to `:8080` | Container not running | `docker ps`; if missing, `docker logs hershield-config-svc` then re-run step 6 |
| `connection timed out` from laptop | Security group missing inbound 8080 | Re-do step 1.5 |
| `permission denied (publickey)` on ssh | Wrong key path or bad permissions | `chmod 400 <pem>`; check the EC2 instance's attached key pair |
| Android app: `Update check failed: ... CLEARTEXT communication ... not permitted` | `usesCleartextTraffic` missing | Confirm `android:usesCleartextTraffic="true"` in `AndroidManifest.xml` |
| Docker build fails with `exec format error` later | AMI architecture and `Dockerfile` base image mismatch | Match: `t2.micro` (x86) ↔ `golang:1.22-alpine` (default), or `t4g.micro` (arm64) ↔ `golang:1.22-alpine` (multi-arch — pulls correct one automatically) |
| ANSI escape codes in `docker logs` look like `^[[32m` garbage | Detached run with `LOG_COLOR=true` | Either set `LOG_COLOR=false`, or run interactive with `-it` as in step 8 |
