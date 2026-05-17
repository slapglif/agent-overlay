# Cloudflare Tunnel pairing for Agent Overlay

Agent Overlay can control high-impact Hermes agents from a phone. Treat a public tunnel as a remote-control surface: expose only the intended Hermes API bridge, require bearer auth, prefer Cloudflare Access, and keep a fast rollback path.

## 1. Hermes API server

On the machine running Hermes, configure the API server with a long random token. Do not paste real tokens into chat or commit them.

```bash
hermes config set API_SERVER_ENABLED true
hermes config set API_SERVER_KEY "<long-random-token>"
hermes gateway
```

Hermes API Server defaults to `http://127.0.0.1:8642` and requires `Authorization: Bearer <API_SERVER_KEY>` for API calls.

## 2. Cloudflare Tunnel

Create a tunnel that maps a single public hostname to the local Hermes API server. Prefer Cloudflare Access in front of the hostname.

Example `cloudflared` config shape:

```yaml
tunnel: hermes-agent-overlay
credentials-file: /path/to/hermes-agent-overlay.json
ingress:
  - hostname: hermes-mobile.example.com
    service: http://127.0.0.1:8642
  - service: http_status:404
```

Example commands, for a human operator to run only after reviewing the hostname/account:

```bash
cloudflared tunnel create hermes-agent-overlay
cloudflared tunnel route dns hermes-agent-overlay hermes-mobile.example.com
cloudflared tunnel --config ./cloudflared-agent-overlay.yml run hermes-agent-overlay
```

Do not tunnel arbitrary local ports, LAN ranges, SSH, Android debug bridge, or raw device-control surfaces.

## 3. Pair the Android app

In Agent Overlay:

1. Set **Gateway URL** to the HTTPS tunnel URL, for example `https://hermes-mobile.example.com`.
2. Set **API key** to the Hermes `API_SERVER_KEY` value.
3. Tap **Connect**.

The app also supports a pairing deep link contract:

```text
agent-overlay://pair?url=https%3A%2F%2Fhermes-mobile.example.com&token=<api-server-key>
```

Pairing links are sensitive because they contain a bearer token. Prefer short-lived tokens and deliver the link only over a trusted channel. Revoke the token after testing or if the link may have leaked.

## 4. App-side safety behavior

Agent Overlay classifies gateway URLs as:

- `LocalDevelopment`: cleartext `http://` allowed only for local development hosts such as `10.0.2.2`, `127.0.0.1`, `localhost`, `0.0.0.0`, and `::1`.
- `RemoteTunnel`: remote endpoints must use `https://`.
- `Blocked`: remote cleartext URLs, unsupported schemes, blank hosts, and URLs with query/fragment content are rejected before network calls.

## 5. Verification sequence

Start with read-only/API-smoke behavior before tool or device-control actions:

1. `GET /v1/models` succeeds through the tunnel.
2. `/api/jobs` list loads in Agent Overlay.
3. A harmless chat request succeeds.
4. Audit logs show the paired user/device and remote IP/session.
5. Only then consider any tool-calling or command passthrough modes.

## 6. Two-way bridge / device-control gate

Do not enable phone/device control just because the API tunnel works. Device-control must require:

- explicit owner consent on the device,
- per-device pairing and revocation,
- least-privilege command allowlists,
- read-only smoke tests before write/control operations,
- audit logs for every action,
- a visible kill switch.

## 7. Rollback

If anything looks wrong:

```bash
cloudflared tunnel route dns delete hermes-agent-overlay hermes-mobile.example.com
cloudflared tunnel delete hermes-agent-overlay
hermes config set API_SERVER_ENABLED false
hermes config set API_SERVER_KEY ""
```

Also revoke any Cloudflare Access sessions and clear/reissue Agent Overlay pairing tokens.
