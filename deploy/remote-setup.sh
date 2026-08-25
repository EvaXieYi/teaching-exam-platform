#!/bin/bash
set -euo pipefail

APP_ROOT=/opt/exam
JAVA11=/usr/lib/jvm/java-11-openjdk

mkdir -p "$APP_ROOT" /etc/nginx/conf.d /etc/maven/conf
mkdir -p /root/.m2

# Maven 走阿里云，避免中央仓库过慢
cat > /root/.m2/settings.xml <<'XML'
<settings>
  <mirrors>
    <mirror>
      <id>aliyun</id>
      <mirrorOf>*</mirrorOf>
      <url>https://maven.aliyun.com/repository/public</url>
    </mirror>
  </mirrors>
</settings>
XML

# npm 国内镜像
npm config set registry https://registry.npmmirror.com

# 后端对内 8080，前端由 Nginx 对外 80
cat > /etc/nginx/conf.d/exam.conf <<'NGINX'
server {
    listen 80 default_server;
    listen [::]:80 default_server;
    server_name _;
    charset utf-8;
    client_max_body_size 20m;
    root /opt/exam/exam-web/dist;
    index index.html;

    location /api/ {
        proxy_pass http://127.0.0.1:8080;
        proxy_http_version 1.1;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        proxy_read_timeout 120s;
    }

    location ~ ^/(swagger-ui|swagger-ui\.html|v3/api-docs|h2-console) {
        proxy_pass http://127.0.0.1:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
    }

    location / {
        try_files $uri $uri/ /index.html;
    }
}
NGINX

# 去掉可能冲突的默认站点（Rocky 自带 listen 80 的 server 块）
rm -f /etc/nginx/conf.d/default.conf
python3 - <<'PY'
from pathlib import Path
p = Path('/etc/nginx/nginx.conf')
text = p.read_text()
start = text.find('    server {\n        listen       80;')
end = text.find('# Settings for a TLS enabled server.')
if start >= 0 and end > start:
    text = text[:start] + '    # default server disabled; exam.conf is the site\n\n' + text[end:]
    p.write_text(text)
    print('nginx default server disabled')
else:
    print('nginx default server already absent')
PY

cat > /etc/systemd/system/exam-server.service <<EOF
[Unit]
Description=Exam platform backend (source-built jar)
After=network.target

[Service]
Type=simple
User=root
WorkingDirectory=${APP_ROOT}/exam-server
Environment=JAVA_HOME=${JAVA11}
Environment=SPRING_PROFILES_ACTIVE=h2
Environment=JAVA_TOOL_OPTIONS=-Dfile.encoding=UTF-8
ExecStart=${JAVA11}/bin/java -jar ${APP_ROOT}/exam-server/target/exam-server-1.0.0.jar --server.address=0.0.0.0 --server.port=8080
Restart=on-failure
RestartSec=5

[Install]
WantedBy=multi-user.target
EOF

echo "setup done"
