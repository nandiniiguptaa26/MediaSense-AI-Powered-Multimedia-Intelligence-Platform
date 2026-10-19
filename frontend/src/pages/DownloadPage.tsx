import React, { useState } from 'react'
import { downloadFile, triggerDownload } from '../services/api'

const MIME_PRESETS: Record<string, string> = {
    mp4: 'video/mp4', mp3: 'audio/mpeg', wav: 'audio/wav',
    webm: 'video/webm', avi: 'video/x-msvideo', wmv: 'video/x-ms-wmv',
    ogg: 'audio/ogg', flac: 'audio/flac', mkv: 'video/x-matroska',
    mov: 'video/quicktime', m4a: 'audio/mp4', txt: 'text/plain',
}

export default function DownloadPage() {
    const [blobName, setBlobName]   = useState('')
    const [outputFormat, setOutputFormat] = useState('mp3')
    const [mimeType, setMimeType]   = useState('audio/mpeg')
    const [loading, setLoading]     = useState(false)
    const [error, setError]         = useState('')
    const [done, setDone]           = useState(false)

    const handleFormatChange = (fmt: string) => {
        setOutputFormat(fmt)
        setMimeType(MIME_PRESETS[fmt] || 'application/octet-stream')
    }

    const submit = async () => {
        if (!blobName) return
        setLoading(true); setError(''); setDone(false)
        try {
            const blob = await downloadFile({ blobName, mimeType, outputFormat })
            const ext = outputFormat || blobName.split('.').pop() || 'bin'
            const base = blobName.includes('.') ? blobName.substring(0, blobName.lastIndexOf('.')) : blobName
            triggerDownload(blob, `${base}.${ext}`)
            setDone(true)
        } catch(e: any) { setError(e.message) }
        finally { setLoading(false) }
    }

    return (
        <div>
            <div className="page-header">
                <h2>⬇️ Download File</h2>
                <p>Retrieve a previously converted file from MinIO and download it to your browser.</p>
            </div>

            <div className="card">
                <div className="card-title">📥 Download Converted File</div>
                <p style={{color:'var(--text-muted)',fontSize:13,marginBottom:16}}>
                    Enter the exact blob name as it appears in the MinIO <strong>converted files</strong> bucket.
                    Typically this is the name returned by a conversion operation (e.g. <code style={{background:'var(--bg3)',padding:'1px 6px',borderRadius:4}}>myvideo.mp4_converted.mp3</code>).
                </p>

                <div className="form-row">
                    <label>Blob Name (in converted-files bucket)</label>
                    <input
                        type="text"
                        placeholder="e.g. myvideo.mp4_converted.mp3"
                        value={blobName}
                        onChange={e => setBlobName(e.target.value)}
                    />
                </div>

                <div className="form-grid">
                    <div className="form-row">
                        <label>Output Format</label>
                        <select value={outputFormat} onChange={e=>handleFormatChange(e.target.value)}>
                            {Object.keys(MIME_PRESETS).map(f=><option key={f}>{f}</option>)}
                        </select>
                    </div>
                    <div className="form-row">
                        <label>MIME Type <span style={{fontWeight:400}}>(auto-set from format)</span></label>
                        <input type="text" value={mimeType} onChange={e=>setMimeType(e.target.value)} placeholder="e.g. audio/mpeg" />
                    </div>
                </div>

                <div className="alert alert-info" style={{marginBottom:16}}>
                    💡 <strong>Blob name pattern:</strong> After conversion, files are saved as <code style={{background:'rgba(0,0,0,0.2)',padding:'1px 6px',borderRadius:4}}>{'{originalName}_converted.{format}'}</code> in the output bucket.
                </div>

                <button className="btn btn-primary btn-full" disabled={!blobName || loading} onClick={submit}>
                    {loading ? <><span className="spinner" />&nbsp;Downloading…</> : '⬇️ Download File'}
                </button>

                {done  && <div className="alert alert-success" style={{marginTop:14}}>✅ File downloaded successfully.</div>}
                {error && <div className="alert alert-error"  style={{marginTop:14}}>❌ {error}</div>}
            </div>

            {/* Quick reference */}
            <div className="card">
                <div className="card-title">📋 Blob Name Reference</div>
                <p style={{color:'var(--text-muted)',fontSize:13,marginBottom:12}}>How files are named in MinIO after each operation:</p>
                <table style={{width:'100%',borderCollapse:'collapse',fontSize:13}}>
                    <thead>
                    <tr style={{borderBottom:'1px solid var(--border)'}}>
                        <th style={{textAlign:'left',padding:'6px 10px',color:'var(--text-muted)',fontWeight:600}}>Operation</th>
                        <th style={{textAlign:'left',padding:'6px 10px',color:'var(--text-muted)',fontWeight:600}}>Blob Name Pattern</th>
                    </tr>
                    </thead>
                    <tbody>
                    {[
                        ['Format Convert',  '{blobName}_converted.{format}'],
                        ['Waveform Video',  '{blobName}_waveform.mp4'],
                        ['Caption Video',   '{blobName}_captioned.mp4'],
                        ['Transcription',   '{blobName}_transcription.txt'],
                    ].map(([op, pattern]) => (
                        <tr key={op} style={{borderBottom:'1px solid var(--border)'}}>
                            <td style={{padding:'8px 10px'}}>{op}</td>
                            <td style={{padding:'8px 10px'}}>
                                <code style={{background:'var(--bg3)',padding:'2px 8px',borderRadius:4,fontSize:12}}>{pattern}</code>
                            </td>
                        </tr>
                    ))}
                    </tbody>
                </table>
            </div>
        </div>
    )
}