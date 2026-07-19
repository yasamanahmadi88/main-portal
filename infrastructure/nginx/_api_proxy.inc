# Common reverse-proxy directives for /api/*.  Included from `default.conf`.
proxy_pass         http://portal_backend_api;
proxy_http_version 1.1;

proxy_set_header   Host              $host;
proxy_set_header   X-Real-IP         $remote_addr;
proxy_set_header   X-Forwarded-For   $proxy_add_x_forwarded_for;
proxy_set_header   X-Forwarded-Proto $forwarded_scheme;
proxy_set_header   X-Forwarded-Host  $host;
proxy_set_header   X-Forwarded-Port  $server_port;
proxy_set_header   Connection        "";

# Cookies rewritten only if the backend accidentally scopes them to itself.
proxy_cookie_domain backend $host;
proxy_cookie_path   / /;

# Prevent caching of API responses at the reverse proxy.
proxy_cache_bypass 1;
proxy_no_cache     1;
proxy_buffering    off;

# Reasonable timeouts; overridden for long-poll endpoints if we add them later.
proxy_connect_timeout 5s;
proxy_send_timeout    30s;
proxy_read_timeout    60s;
