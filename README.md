# chatgpt-api

Unofficial API in Java (bypassing Cloudflare v2 challenge CAPTCHA
using [undetected_chromedriver](https://github.com/ultrafunkamsterdam/undetected-chromedriver)).

### Environment variables

| key            | value            | remark                                         |
|----------------|------------------|------------------------------------------------|
| WEB_DRIVER_URL | http://host:9515 | The undetected chrome driver.                  |
| PROXY_SERVER   | host:port        | 127.0.0.1:12345 / **socks5**://127.0.0.1:12345 |