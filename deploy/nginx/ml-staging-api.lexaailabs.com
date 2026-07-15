server {
    server_name ml-staging-api.lexaailabs.com;
    client_max_body_size 60m;

    add_header X-Robots-Tag "noindex, nofollow, noarchive" always;

    location / {
        proxy_pass http://127.0.0.1:8001;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        proxy_connect_timeout 30s;
        proxy_send_timeout 300s;
        proxy_read_timeout 300s;
    }

    location ~ ^/(lab|admin)/ {
        auth_basic "MurrLex staging";
        auth_basic_user_file /etc/nginx/.murrlex_htpasswd;

        proxy_pass http://127.0.0.1:8001;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        proxy_connect_timeout 30s;
        proxy_send_timeout 300s;
        proxy_read_timeout 300s;
    }

    listen 443 ssl;
    ssl_certificate /etc/letsencrypt/live/ml-staging-api.lexaailabs.com/fullchain.pem;
    ssl_certificate_key /etc/letsencrypt/live/ml-staging-api.lexaailabs.com/privkey.pem;
    include /etc/letsencrypt/options-ssl-nginx.conf;
    ssl_dhparam /etc/letsencrypt/ssl-dhparams.pem;
}

server {
    if ($host = ml-staging-api.lexaailabs.com) {
        return 301 https://$host:8443$request_uri;
    }

    listen 80;
    server_name ml-staging-api.lexaailabs.com;
    return 404;
}
