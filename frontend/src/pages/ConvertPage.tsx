import React, { useState, useRef } from 'react'
import { convertMedia, generateWaveform, generateCaption, transcribeMedia, triggerDownload } from '../services/api'

const OUTPUT_FORMATS = ['mp4','mp3','wav','webm','avi','wmv','weba','ogg','flac','mkv','mov','m4a']

export default function ConvertPage() {
    const [activeTab, setActiveTab] = useState<'convert'|'waveform'|'caption'|'transcribe'>('convert')
    return (
        <div>
            <div className="page-header">
                <h2>🔄 Convert Media</h2>
                <p>Run FFmpeg-powered conversions on files already uploaded to MinIO. Results download directly to your browser.</p>
            </div>
            <div className="tabs">
                <button className={`tab ${activeTab==='convert'  ?'active':''}`} onClick={()=>setActiveTab('convert')}>Format Convert</button>
                <button className={`tab ${activeTab==='waveform' ?'active':''}`} onClick={()=>setActiveTab('waveform')}>Waveform Video</button>
                <button className={`tab ${activeTab==='caption'  ?'active':''}`} onClick={()=>setActiveTab('caption')}>Add Captions</button>
                <button className={`tab ${activeTab==='transcribe'?'active':''}`} onClick={()=>setActiveTab('transcribe')}>Transcribe</button>
            </div>
            {activeTab === 'convert'   && <ConvertTab />}
            {activeTab === 'waveform'  && <WaveformTab />}
            {activeTab === 'caption'   && <CaptionTab />}
            {activeTab === 'transcribe'&& <TranscribeTab />}
        </div>
    )
}

// ── Format Convert ────────────────────────────────────────────────────────────

function ConvertTab() {
    const [blobName, setBlobName] = useState('')
    const [format, setFormat]   = useState('mp3')
    const [mimeType, setMimeType] = useState('')
    const [loading, setLoading] = useState(false)
    const [error, setError]     = useState('')
    const [done, setDone]       = useState(false)

    const submit = async () => {
        if (!blobName || !format) return
        setLoading(true); setError(''); setDone(false)
        try {
            const blob = await convertMedia({ blobName, format, mimeType: mimeType||undefined })
            triggerDownload(blob, `${blobName}_converted.${format}`)
            setDone(true)
        } catch(e: any) { setError(e.message) }
        finally { setLoading(false) }
    }

    return (
        <div className="card">
            <div className="card-title">🎬 Convert Format</div>
            <p style={{color:'var(--text-muted)',fontSize:13,marginBottom:16}}>
                Enter the exact blob name from MinIO (returned by Upload). The converted file will download to your browser and is also saved in MinIO.
            </p>

            <div className="form-row">
                <label>Blob Name (file in MinIO)</label>
                <input type="text" placeholder="e.g. myvideo.mp4" value={blobName} onChange={e=>setBlobName(e.target.value)} />
            </div>

            <div className="form-grid">
                <div className="form-row">
                    <label>Target Format</label>
                    <select value={format} onChange={e=>setFormat(e.target.value)}>
                        {OUTPUT_FORMATS.map(f=><option key={f}>{f}</option>)}
                    </select>
                </div>
                <div className="form-row">
                    <label>MIME Type <span style={{fontWeight:400}}>(optional — auto-detected)</span></label>
                    <input type="text" placeholder="e.g. audio/mpeg" value={mimeType} onChange={e=>setMimeType(e.target.value)} />
                </div>
            </div>

            <button className="btn btn-primary btn-full" disabled={!blobName||loading} onClick={submit}>
                {loading ? <><span className="spinner"/>&nbsp;Converting… (this may take a while)</> : '🔄 Convert & Download'}
            </button>

            {done  && <div className="alert alert-success">✅ Conversion done — file downloaded.</div>}
            {error && <div className="alert alert-error">❌ {error}</div>}
        </div>
    )
}

// ── Waveform Video ────────────────────────────────────────────────────────────

function WaveformTab() {
    const [blobName, setBlobName]         = useState('')
    const [bgImage, setBgImage]           = useState<File|null>(null)
    const [loading, setLoading]           = useState(false)
    const [error, setError]               = useState('')
    const [done, setDone]                 = useState(false)
    const imgRef = useRef<HTMLInputElement>(null)

    const submit = async () => {
        if (!blobName) return
        setLoading(true); setError(''); setDone(false)
        try {
            const blob = await generateWaveform(blobName, bgImage||undefined)
            triggerDownload(blob, `${blobName}_waveform.mp4`)
            setDone(true)
        } catch(e: any) { setError(e.message) }
        finally { setLoading(false) }
    }

    return (
        <div className="card">
            <div className="card-title">🌊 Waveform Visualizer</div>
            <p style={{color:'var(--text-muted)',fontSize:13,marginBottom:16}}>
                Generates an MP4 video showing the audio waveform. Optionally provide a background image.
            </p>

            <div className="form-row">
                <label>Audio Blob Name in MinIO</label>
                <input type="text" placeholder="e.g. audio.mp3" value={blobName} onChange={e=>setBlobName(e.target.value)} />
            </div>

            <div className="form-row">
                <label>Background Image <span style={{fontWeight:400}}>(optional)</span></label>
                <div style={{display:'flex',gap:10,alignItems:'center'}}>
                    <button className="btn btn-outline btn-sm" onClick={()=>imgRef.current?.click()}>
                        {bgImage ? `🖼️ ${bgImage.name}` : '📂 Choose Image'}
                    </button>
                    {bgImage && <button className="btn btn-sm" style={{color:'var(--error)',background:'none',border:'none',cursor:'pointer'}} onClick={()=>setBgImage(null)}>✕ Remove</button>}
                    <input ref={imgRef} type="file" accept="image/*" style={{display:'none'}} onChange={e=>setBgImage(e.target.files?.[0]||null)} />
                </div>
            </div>

            <button className="btn btn-primary btn-full" disabled={!blobName||loading} onClick={submit}>
                {loading ? <><span className="spinner"/>&nbsp;Generating waveform…</> : '🌊 Generate & Download'}
            </button>

            {done  && <div className="alert alert-success">✅ Waveform video downloaded.</div>}
            {error && <div className="alert alert-error">❌ {error}</div>}
        </div>
    )
}

// ── Caption Video ─────────────────────────────────────────────────────────────

function CaptionTab() {
    const [blobName, setBlobName] = useState('')
    const [loading, setLoading]   = useState(false)
    const [error, setError]       = useState('')
    const [done, setDone]         = useState(false)

    const submit = async () => {
        if (!blobName) return
        setLoading(true); setError(''); setDone(false)
        try {
            const blob = await generateCaption(blobName)
            triggerDownload(blob, `${blobName}_captioned.mp4`)
            setDone(true)
        } catch(e: any) { setError(e.message) }
        finally { setLoading(false) }
    }

    return (
        <div className="card">
            <div className="card-title">💬 Auto-Caption Video</div>
            <p style={{color:'var(--text-muted)',fontSize:13,marginBottom:16}}>
                Transcribes your video using Whisper, then burns the captions directly into the video as subtitles.
            </p>

            <div className="form-row">
                <label>Video Blob Name in MinIO</label>
                <input type="text" placeholder="e.g. myvideo.mp4" value={blobName} onChange={e=>setBlobName(e.target.value)} />
            </div>

            <div className="alert alert-info" style={{marginBottom:16}}>
                ℹ️ This process runs Whisper transcription + FFmpeg subtitle burn. It can take several minutes for long videos.
            </div>

            <button className="btn btn-primary btn-full" disabled={!blobName||loading} onClick={submit}>
                {loading ? <><span className="spinner"/>&nbsp;Transcribing & captioning…</> : '💬 Generate Captions & Download'}
            </button>

            {done  && <div className="alert alert-success">✅ Captioned video downloaded.</div>}
            {error && <div className="alert alert-error">❌ {error}</div>}
        </div>
    )
}

// ── Transcribe ────────────────────────────────────────────────────────────────

function TranscribeTab() {
    const [blobName, setBlobName] = useState('')
    const [loading, setLoading]   = useState(false)
    const [error, setError]       = useState('')
    const [transcript, setTranscript] = useState('')

    const submit = async () => {
        if (!blobName) return
        setLoading(true); setError(''); setTranscript('')
        try {
            const blob = await transcribeMedia(blobName)
            const text = await blob.text()
            setTranscript(text)
            triggerDownload(blob, `${blobName}_transcription.txt`)
        } catch(e: any) { setError(e.message) }
        finally { setLoading(false) }
    }

    return (
        <div className="card">
            <div className="card-title">📝 Transcribe Audio / Video</div>
            <p style={{color:'var(--text-muted)',fontSize:13,marginBottom:16}}>
                Uses OpenAI Whisper to convert speech to text. The .txt file downloads automatically and is also saved to MinIO.
            </p>

            <div className="form-row">
                <label>Audio/Video Blob Name in MinIO</label>
                <input type="text" placeholder="e.g. podcast.mp3" value={blobName} onChange={e=>setBlobName(e.target.value)} />
            </div>

            <button className="btn btn-primary btn-full" disabled={!blobName||loading} onClick={submit}>
                {loading ? <><span className="spinner"/>&nbsp;Transcribing…</> : '📝 Transcribe & Download'}
            </button>

            {error && <div className="alert alert-error">❌ {error}</div>}

            {transcript && (
                <>
                    <div className="alert alert-success" style={{marginTop:14}}>✅ Transcription complete — .txt file downloaded.</div>
                    <label style={{marginTop:14,marginBottom:6,display:'block'}}>Preview</label>
                    <div className="result-box">{transcript}</div>
                </>
            )}
        </div>
    )
}