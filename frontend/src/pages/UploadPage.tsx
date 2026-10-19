import React, { useState, useRef, useCallback } from 'react'
import { uploadSingle, uploadBatch, UploadResult } from '../services/api'

const OUTPUT_FORMATS = ['mp4','mp3','wav','webm','avi','wmv','weba']

export default function UploadPage() {
    const [activeTab, setActiveTab] = useState<'single'|'batch'>('single')
    return (
        <div>
            <div className="page-header">
                <h2>📤 Upload Media</h2>
                <p>Upload audio or video files to MinIO storage. A conversion job is triggered automatically.</p>
            </div>
            <div className="tabs">
                <button className={`tab ${activeTab==='single'?'active':''}`} onClick={()=>setActiveTab('single')}>Single File</button>
                <button className={`tab ${activeTab==='batch' ?'active':''}`} onClick={()=>setActiveTab('batch')}>Batch Upload</button>
            </div>
            {activeTab === 'single' ? <SingleUpload /> : <BatchUpload />}
        </div>
    )
}

// ── Single upload ─────────────────────────────────────────────────────────────

function SingleUpload() {
    const [file, setFile] = useState<File|null>(null)
    const [format, setFormat] = useState('mp3')
    const [loading, setLoading] = useState(false)
    const [result, setResult] = useState<UploadResult|null>(null)
    const [dragging, setDragging] = useState(false)
    const inputRef = useRef<HTMLInputElement>(null)

    const handleDrop = useCallback((e: React.DragEvent) => {
        e.preventDefault(); setDragging(false)
        const f = e.dataTransfer.files[0]
        if (f) setFile(f)
    }, [])

    const submit = async () => {
        if (!file) return
        setLoading(true); setResult(null)
        try {
            const r = await uploadSingle(file, format)
            setResult(r)
        } catch(e: any) {
            setResult({ failed: [{ fileName: file.name, reason: e.message }], timestamp: new Date().toISOString() })
        } finally { setLoading(false) }
    }

    const success = result && result.uploaded && !result.failed

    return (
        <div className="card">
            <div className="card-title">📁 Single File Upload</div>

            <div className="form-row">
                <div
                    className={`drop-zone ${dragging?'dragging':''}`}
                    onDragOver={e=>{e.preventDefault();setDragging(true)}}
                    onDragLeave={()=>setDragging(false)}
                    onDrop={handleDrop}
                    onClick={()=>inputRef.current?.click()}
                >
                    <div className="drop-zone-icon">{file ? '🎵' : '☁️'}</div>
                    {file
                        ? <p><strong>{file.name}</strong> ({(file.size/1024/1024).toFixed(2)} MB)</p>
                        : <p>Drag & drop a file or <strong>click to browse</strong></p>
                    }
                    <p style={{fontSize:11,marginTop:6,color:'var(--text-muted)'}}>Allowed: mp4, webm, avi, wmv, mp3, wav, weba</p>
                    <input ref={inputRef} type="file" accept=".mp4,.webm,.avi,.wmv,.mp3,.wav,.weba" style={{display:'none'}}
                           onChange={e=>{ if(e.target.files?.[0]) setFile(e.target.files[0]) }} />
                </div>
            </div>

            <div className="form-row">
                <label>Output Format (conversion target)</label>
                <select value={format} onChange={e=>setFormat(e.target.value)}>
                    {OUTPUT_FORMATS.map(f=><option key={f}>{f}</option>)}
                </select>
            </div>

            <button className="btn btn-primary btn-full" disabled={!file||loading} onClick={submit}>
                {loading ? <><span className="spinner"/>&nbsp;Uploading…</> : '⬆️  Upload File'}
            </button>

            {result && (
                <div className={`alert ${success?'alert-success':'alert-error'}`}>
                    {success
                        ? <span>✅ Uploaded as <strong>{result.uploaded as string}</strong>. Conversion job queued.</span>
                        : <span>❌ {(result.failed as any)?.reason || 'Upload failed'}</span>
                    }
                </div>
            )}
        </div>
    )
}

// ── Batch upload ──────────────────────────────────────────────────────────────

function BatchUpload() {
    const [files, setFiles] = useState<File[]>([])
    const [format, setFormat] = useState('mp3')
    const [loading, setLoading] = useState(false)
    const [result, setResult] = useState<UploadResult|null>(null)
    const [dragging, setDragging] = useState(false)
    const inputRef = useRef<HTMLInputElement>(null)

    const addFiles = (newFiles: FileList|null) => {
        if (!newFiles) return
        setFiles(prev => {
            const existing = new Set(prev.map(f=>f.name))
            const toAdd = Array.from(newFiles).filter(f=>!existing.has(f.name))
            return [...prev, ...toAdd]
        })
    }

    const remove = (name: string) => setFiles(prev => prev.filter(f=>f.name!==name))

    const submit = async () => {
        if (!files.length) return
        setLoading(true); setResult(null)
        try {
            const r = await uploadBatch(files, format)
            setResult(r)
        } catch(e: any) {
            setResult({ uploaded: [], failed: [{ fileName: 'batch', reason: e.message }], timestamp: new Date().toISOString() })
        } finally { setLoading(false) }
    }

    const uploaded = result?.uploaded as string[] | undefined
    const failed   = result?.failed   as any[]     | undefined

    return (
        <div className="card">
            <div className="card-title">📂 Batch Upload</div>

            <div className="form-row">
                <div
                    className={`drop-zone ${dragging?'dragging':''}`}
                    onDragOver={e=>{e.preventDefault();setDragging(true)}}
                    onDragLeave={()=>setDragging(false)}
                    onDrop={e=>{e.preventDefault();setDragging(false);addFiles(e.dataTransfer.files)}}
                    onClick={()=>inputRef.current?.click()}
                >
                    <div className="drop-zone-icon">📦</div>
                    <p>Drag & drop <strong>multiple files</strong> or <strong>click to browse</strong></p>
                    <input ref={inputRef} type="file" multiple accept=".mp4,.webm,.avi,.wmv,.mp3,.wav,.weba" style={{display:'none'}}
                           onChange={e=>addFiles(e.target.files)} />
                </div>
            </div>

            {files.length > 0 && (
                <div className="form-row">
                    <label>{files.length} file{files.length>1?'s':''} selected</label>
                    <ul className="file-list">
                        {files.map(f=>(
                            <li key={f.name} className="file-item">
                                <span className="file-item-name">🎵 {f.name}</span>
                                <span style={{color:'var(--text-muted)',fontSize:12,marginRight:8}}>{(f.size/1024/1024).toFixed(2)} MB</span>
                                <button className="file-item-remove" onClick={()=>remove(f.name)}>×</button>
                            </li>
                        ))}
                    </ul>
                </div>
            )}

            <div className="form-row">
                <label>Output Format (applied to all files)</label>
                <select value={format} onChange={e=>setFormat(e.target.value)}>
                    {OUTPUT_FORMATS.map(f=><option key={f}>{f}</option>)}
                </select>
            </div>

            <button className="btn btn-primary btn-full" disabled={!files.length||loading} onClick={submit}>
                {loading ? <><span className="spinner"/>&nbsp;Uploading…</> : `⬆️  Upload ${files.length||''} File${files.length!==1?'s':''}`}
            </button>

            {result && (
                <div style={{marginTop:14}}>
                    {uploaded && uploaded.length > 0 && (
                        <div className="alert alert-success">
                            ✅ <strong>{uploaded.length}</strong> file{uploaded.length>1?'s':''} uploaded: {uploaded.join(', ')}
                        </div>
                    )}
                    {failed && failed.length > 0 && (
                        <div className="alert alert-error" style={{marginTop:8}}>
                            ❌ <strong>{failed.length}</strong> failed: {failed.map((f:any)=>`${f.file||f.fileName}: ${f.error||f.reason}`).join('; ')}
                        </div>
                    )}
                </div>
            )}
        </div>
    )
}