/**
 * Shield DNS Cloud Pre-compiler Pipeline
 * Google Cloud Function (Node.js) or Scheduled Cloud Run Job.
 *
 * Downloads major open-source DNS blocklists, parses and cleans comments,
 * de-duplicates, categorizes, generates version metadata with checksums,
 * and saves compressed outputs to Google Cloud Storage or Firebase Hosting.
 */

const axios = require('axios');
const zlib = require('zlib');
const crypto = require('crypto');
const { Storage } = require('@google-cloud/storage');

const storage = new Storage();
const BUCKET_NAME = process.env.BLOCKLIST_BUCKET || 'trustphone-dns-blocklists';

// Blocklist Sources
const SOURCES = {
    STEVEN_BLACK: 'https://raw.githubusercontent.com/StevenBlack/hosts/master/hosts',
    ADAWAY: 'https://adaway.org/hosts.txt',
    URLHAUS: 'https://urlhaus.abuse.ch/downloads/hostfile/',
    PHISHING_ARMY: 'https://phishing.army/download/phishing_army_blocklist.txt',
    EASYSUB: 'https://v.firebog.net/hosts/AdguardDNS.txt'
};

// Categorization Keywords
const KEYWORDS = {
    malware: ['malware', 'virus', 'ransomware', 'trojan', 'spyware', 'badware', 'c2', 'dropper'],
    phishing: ['phish', 'spoof', 'login-update', 'phishing'],
    scam: ['scam', 'fraud', 'fake', 'scams'],
    adult: ['porn', 'adult', 'sex', 'xxx', 'erotic', 'nsfw', 'tube'],
    gambling: ['gamble', 'casino', 'betting', 'poker', 'lottery'],
    telemetry: ['telemetry', 'metrics', 'analytics', 'stats', 'beacon'],
    cryptoscam: ['crypto', 'miner', 'cryptominer', 'coinhive', 'minergate'],
    ads: ['doubleclick', 'adservice', 'adsystem', 'adserver', 'banner', 'ads']
};

exports.precompileBlocklists = async (req, res) => {
    try {
        console.log('Starting blocklist precompile job...');
        const uniqueDomains = new Set();
        const domainsByCategory = {
            ads: [],
            trackers: [],
            malware: [],
            phishing: [],
            scam: [],
            adult: [],
            gambling: [],
            telemetry: [],
            cryptoscam: []
        };

        // Download and parse all lists
        for (const [name, url] of Object.entries(SOURCES)) {
            try {
                console.log(`Downloading ${name} from ${url}...`);
                const response = await axios.get(url, { timeout: 30000 });
                const count = parseHostsContent(response.data, uniqueDomains, domainsByCategory);
                console.log(`Parsed ${count} unique domains from ${name}`);
            } catch (err) {
                console.error(`Failed to download ${name}: ${err.message}`);
            }
        }

        // Clean arrays
        const totalCount = uniqueDomains.size;
        console.log(`Total consolidated unique domains: ${totalCount}`);

        // Write grouped domain files and compress them
        const filesMetadata = {};
        for (const [category, list] of Object.entries(domainsByCategory)) {
            const uniqueList = [...new Set(list)];
            const fileContent = uniqueList.join('\n');
            const gzipped = zlib.gzipSync(fileContent);

            // Compute hash checksum of gzipped payload
            const sha256 = crypto.createHash('sha256').update(gzipped).digest('hex');
            const fileName = `blocklist_${category}.txt.gz`;

            // Upload to Google Cloud Storage
            await uploadToGCS(fileName, gzipped, 'application/gzip');

            filesMetadata[category] = {
                file_name: fileName,
                domain_count: uniqueList.length,
                sha256_checksum: sha256,
                download_url: `https://storage.googleapis.com/${BUCKET_NAME}/${fileName}`
            };
        }

        // Create and upload metadata JSON
        const metadata = {
            version: Date.now(),
            created_at: new Date().toISOString(),
            total_domain_count: totalCount,
            categories: filesMetadata,
            licenses: {
                steven_black: 'MIT License',
                adaway: 'CC BY 3.0',
                urlhaus: 'CC0 1.0 Universal',
                phishing_army: 'Public Domain / Free for personal use',
                easylist: 'CC BY-SA 3.0'
            }
        };

        const metadataString = JSON.stringify(metadata, null, 2);
        const metadataBuffer = Buffer.from(metadataString, 'utf-8');
        await uploadToGCS('metadata.json', metadataBuffer, 'application/json');

        console.log('Blocklist precompile job completed successfully!');
        if (res) {
            res.status(200).send({
                success: true,
                message: 'Precompile successful',
                version: metadata.version,
                total_domains: totalCount
            });
        }
    } catch (error) {
        console.error('Job failed with error:', error);
        if (res) {
            res.status(500).send({ success: false, error: error.message });
        }
    }
};

function parseHostsContent(text, uniqueSet, categoryMap) {
    let parsedCount = 0;
    const lines = text.split('\n');
    for (let line of lines) {
        // Remove comments
        line = line.split('#')[0].split('!')[0].trim();
        if (!line) continue;

        let domain = '';
        if (line.startsWith('||')) {
            domain = line.replace('||', '').split('^')[0].split('/')[0];
        } else {
            const parts = line.split(/\s+/);
            if (parts.length >= 2 && (parts[0] === '0.0.0.0' || parts[0] === '127.0.0.1')) {
                domain = parts[1];
            } else if (parts.length === 1) {
                domain = parts[0];
            }
        }

        domain = domain.toLowerCase().replace(/\.$/, '');
        if (domain && isValidDomain(domain) && !uniqueSet.has(domain)) {
            uniqueSet.add(domain);
            parsedCount++;

            // Categorize
            const category = categorize(domain);
            categoryMap[category].push(domain);
        }
    }
    return parsedCount;
}

function categorize(domain) {
    for (const [category, keywords] of Object.entries(KEYWORDS)) {
        if (keywords.some(keyword => domain.includes(keyword))) {
            return category;
        }
    }
    return 'trackers'; // Fallback default
}

function isValidDomain(domain) {
    if (domain.length > 253) return false;
    const regex = /^[a-z0-9]+([\-\.]{1}[a-z0-9]+)*\.[a-z]{2,5}$/;
    return regex.test(domain) && domain.includes('.');
}

async function uploadToGCS(fileName, data, contentType) {
    const bucket = storage.bucket(BUCKET_NAME);
    const file = bucket.file(fileName);
    await file.save(data, {
        metadata: { contentType: contentType },
        resumable: false
    });
    console.log(`Uploaded file: ${fileName} to bucket: ${BUCKET_NAME}`);
}
