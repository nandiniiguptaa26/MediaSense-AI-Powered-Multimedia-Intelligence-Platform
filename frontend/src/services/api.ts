// Base URLs — vite proxy rewrites these to the actual service ports
const UPLOAD_BASE   = '/upload'
const CONVERT_BASE  = '/convert'
const DOWNLOAD_BASE = '/download'
const QA_BASE       = '/qna'

// ── Types ────────────────────────────────────────────────────────────────────

export interface UploadResult {
    uploaded?: string | string[]
    failed?: { fileName?: string; file?: string; reason?: string; error?: string }[] | { fileName: string; reason: string }
    timestamp: string
}

export interface ConvertRequest {
    blobName: string
    format: string
    mimeType?: string
    codecs?: Record<string, string>
    arguments?: Record<string, string>
}

export interface DownloadRequest {
    blobName: string
    mimeType: string
    outputFormat: string
}

export interface IngestResponse {
    source: string
    status: string
    charactersIndexed: number
}

export interface AskResponse {
    question: string
    answer: string
    modelUsed: string
}

export interface ContextStatus {
    chunksLoaded: number
    sources: string[]
}

// ── Upload Service ────────────────────────────────────────────────────────────

export async function uploadSingle(file: File, outputFormat: string): Promise<UploadResult> {
    const form = new FormData()
    form.append('file', file)
    form.append('outputFormat', outputFormat)
    const res = await fetch(`${UPLOAD_BASE}`, { method: 'POST', body: form })
    return res.json()
}

export async function uploadBatch(files: File[], outputFormat: string): Promise<UploadResult> {
    const form = new FormData()
    files.forEach(f => form.append('files', f))
    form.append('outputFormat', outputFormat)
    const res = await fetch(`${UPLOAD_BASE}/batch`, { method: 'POST', body: form })
    return res.json()
}

// ── Conversion Service ────────────────────────────────────────────────────────

export async function convertMedia(req: ConvertRequest): Promise<Blob> {
    const form = new FormData()
    form.append('blobName', req.blobName)
    form.append('format', req.format)
    if (req.mimeType) form.append('mimeType', req.mimeType)
    const controller = new AbortController()
    const timeout = setTimeout(() => controller.abort(), 3000000)
    try {
        const res = await fetch(`${CONVERT_BASE}`, { method: 'POST', body: form, signal: controller.signal })
        if (!res.ok) throw new Error(`Conversion failed: HTTP ${res.status}`)
        return res.blob()
    } finally {
        clearTimeout(timeout)
    }
}

export async function generateWaveform(blobName: string, backgroundImage?: File): Promise<Blob> {
    const form = new FormData()
    form.append('blobName', blobName)
    if (backgroundImage) form.append('backgroundImage', backgroundImage)
    const controller = new AbortController()
    const timeout = setTimeout(() => controller.abort(), 3000000)
    try {
        const res = await fetch(`${CONVERT_BASE}/waveform`, { method: 'POST', body: form, signal: controller.signal })
        if (!res.ok) throw new Error(`Waveform generation failed: HTTP ${res.status}`)
        return res.blob()
    } finally {
        clearTimeout(timeout)
    }
}

export async function generateCaption(blobName: string): Promise<Blob> {
    const form = new FormData()
    form.append('blobName', blobName)
    const controller = new AbortController()
    const timeout = setTimeout(() => controller.abort(), 3000000)
    try {
        const res = await fetch(`${CONVERT_BASE}/caption`, { method: 'POST', body: form, signal: controller.signal })
        if (!res.ok) throw new Error(`Caption generation failed: HTTP ${res.status}`)
        return res.blob()
    } finally {
        clearTimeout(timeout)
    }
}

export async function transcribeMedia(blobName: string): Promise<Blob> {
    const form = new FormData()
    form.append('blobName', blobName)
    const controller = new AbortController()
    const timeout = setTimeout(() => controller.abort(), 3000000)
    try {
        const res = await fetch(`${CONVERT_BASE}/transcribe`, { method: 'POST', body: form, signal: controller.signal })
        if (!res.ok) throw new Error(`Transcription failed: HTTP ${res.status}`)
        return res.blob()
    } finally {
        clearTimeout(timeout)
    }
}

// ── Download Service ──────────────────────────────────────────────────────────

export async function downloadFile(req: DownloadRequest): Promise<Blob> {
    const res = await fetch(`${DOWNLOAD_BASE}`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(req)
    })
    if (!res.ok) throw new Error(`Download failed: HTTP ${res.status}`)
    return res.blob()
}

// ── Q&A Service ───────────────────────────────────────────────────────────────

export async function ingestFile(file: File): Promise<IngestResponse> {
    const form = new FormData()
    form.append('file', file)
    const res = await fetch(`${QA_BASE}/ingest`, { method: 'POST', body: form })
    if (!res.ok) throw new Error(`Ingest failed: HTTP ${res.status}`)
    return res.json()
}

export async function ingestUrl(url: string): Promise<IngestResponse> {
    const res = await fetch(`${QA_BASE}/ingest-url`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ url })
    })
    if (!res.ok) throw new Error(`URL ingest failed: HTTP ${res.status}`)
    return res.json()
}

export async function askQuestion(question: string): Promise<AskResponse> {
    const res = await fetch(`${QA_BASE}/ask`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ question })
    })
    if (!res.ok) throw new Error(`Ask failed: HTTP ${res.status}`)
    return res.json()
}

export async function clearContext(): Promise<{ status: string; message: string }> {
    const res = await fetch(`${QA_BASE}/context`, { method: 'DELETE' })
    return res.json()
}

export async function getContextStatus(): Promise<ContextStatus> {
    const res = await fetch(`${QA_BASE}/context/status`)
    return res.json()
}

// ── Helpers ───────────────────────────────────────────────────────────────────

export function triggerDownload(blob: Blob, filename: string) {
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = filename
    a.click()
    URL.revokeObjectURL(url)
}