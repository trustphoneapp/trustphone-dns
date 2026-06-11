# Custom Domain Setup — Namecheap

**Primary domain:** `trustphonedns.app`  
**Also owned:** `trustphonedns.com`, `trustphonedns.org`

## Recommended layout

| Domain | Use |
|--------|-----|
| **trustphonedns.app** | Main site + Play Store privacy policy |
| **trustphonedns.com** | Redirect → trustphonedns.app (optional) |
| **trustphonedns.org** | Redirect → trustphonedns.app (optional) |

**Play Store privacy URL:** `https://trustphonedns.app/privacy.html`

---

## Step 1 — GitHub Pages custom domain

1. Repo → [Settings → Pages](https://github.com/trustphoneapp/trustphone-dns/settings/pages)
2. Branch: `main` → folder `/docs`
3. Under **Custom domain**, enter: `trustphonedns.app`
4. Click **Save**
5. Wait for DNS check, then enable **Enforce HTTPS**

The `docs/CNAME` file in this repo already contains `trustphonedns.app`.

---

## Step 2 — Namecheap DNS for trustphonedns.app

1. Namecheap → **Domain List** → **Manage** next to `trustphonedns.app`
2. **Advanced DNS** tab
3. Delete any conflicting A/CNAME records for `@` and `www`
4. Add these records:

| Type | Host | Value | TTL |
|------|------|-------|-----|
| **A Record** | `@` | `185.199.108.153` | Automatic |
| **A Record** | `@` | `185.199.109.153` | Automatic |
| **A Record** | `@` | `185.199.110.153` | Automatic |
| **A Record** | `@` | `185.199.111.153` | Automatic |
| **CNAME** | `www` | `trustphoneapp.github.io.` | Automatic |

> Note the trailing dot on the CNAME value — some UIs add it automatically.

DNS can take **15 minutes to 48 hours** to propagate. GitHub will show a green check when it's ready.

---

## Step 3 — Redirect .com and .org (optional)

In Namecheap for **trustphonedns.com** and **trustphonedns.org**:

1. **Domain** tab → **Redirect Domain**
2. Redirect to: `https://trustphonedns.app`
3. Type: **Permanent (301)**

Or use URL Redirect records in Advanced DNS:

| Type | Host | Value |
|------|------|-------|
| URL Redirect | `@` | `https://trustphonedns.app` |
| URL Redirect | `www` | `https://trustphonedns.app` |

---

## Step 4 — Email (optional)

Set up `support@trustphonedns.app` in Namecheap → **Private Email** or forward to your Gmail:

| Type | Host | Value |
|------|------|-------|
| CNAME | `mail` | (per Namecheap email setup) |
| TXT | `@` | SPF record from Namecheap |

---

## URLs after setup

| Page | URL |
|------|-----|
| Home | https://trustphonedns.app/ |
| Privacy policy | https://trustphonedns.app/privacy.html |

Use the privacy URL in **Play Console → App content → Privacy policy**.
