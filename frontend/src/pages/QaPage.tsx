import React, { useState, useEffect, useRef, useCallback } from 'react'
import {
    ingestFile, ingestUrl, askQuestion, clearContext, getContextStatus,
    IngestResponse, AskResponse, ContextStatus
} from '../services/api'

interface ChatMessage {
    role: 'user' | 'assistant'
    text: string
    modelUsed?: string
}

export default function QaPage() {
    const [activeTab, setActiveTab] = useState<'chat'|'ingest'|'context'>('ingest')
    const [contextStatus, setContextStatus] = useState<ContextStatus>({ chunksLoaded: 0, sources: [] })

    const refreshStatus = useCallback(async () => {
        try { setContextStatus(await getContextStatus()) } catch {}
    }, [])

    useEffect(() => { refreshStatus() }, [refreshStatus])

    return (
        <div>
            <div className="page-header">
                <h2>🤖 Q&A Intelligence</h2>
                <p>Ingest content from files or URLs, then ask questions. Powered by Ollama (local LLM).</p>
            </div>

            {/* Status bar */}
            <div className="stat-row">
                <div className="stat-box">
                    <div className="stat-value">{contextStatus.chunksLoaded}</div>
                    <div className="stat-label">Context Chunks Loaded</div>
                </div>
                <div className="stat-box" style={{flex:3}}>
                    <div style={{fontSize:12,color:'var(--text-muted)',marginBottom:6,fontWeight:600}}>Sources</div>
                    {contextStatus.sources.length === 0
                        ? <span style={{color:'var(--text-muted)',fontSize:13}}>No sources ingested yet</span>
                        : <div className="source-list">
                            {contextStatus.sources.map(s=>(
                                <span key={s} className="source-tag">📄 {s.length>40?s.slice(0,38)+'…':s}</span>
                            ))}
                        </div>
                    }
                </div>
            </div>

            <div className="tabs">
                <button className={`tab ${activeTab==='ingest' ?'active':''}`} onClick={()=>setActiveTab('ingest')}>📥 Ingest Content</button>
                <button className={`tab ${activeTab==='chat'   ?'active':''}`} onClick={()=>setActiveTab('chat')}>💬 Ask Questions</button>
                <button className={`tab ${activeTab==='context'?'active':''}`} onClick={()=>setActiveTab('context')}>⚙️ Manage Context</button>
            </div>

            {activeTab === 'ingest'  && <IngestTab onIngested={refreshStatus} />}
            {activeTab === 'chat'    && <ChatTab contextLoaded={contextStatus.chunksLoaded > 0} />}
            {activeTab === 'context' && <ContextTab onCleared={refreshStatus} sources={contextStatus.sources} />}
        </div>
    )
}

// ── Ingest Tab ────────────────────────────────────────────────────────────────

function IngestTab({ onIngested }: { onIngested: () => void }) {
    const [subTab, setSubTab] = useState<'file'|'url'>('file')
    return (
        <div className="card">
            <div className="card-title">📥 Ingest Content</div>
            <div style={{display:'flex',gap:8,marginBottom:20}}>
                <button className={`btn ${subTab==='file'?'btn-primary':'btn-outline'} btn-sm`} onClick={()=>setSubTab('file')}>📄 From File</button>
                <button className={`btn ${subTab==='url' ?'btn-primary':'btn-outline'} btn-sm`} onClick={()=>setSubTab('url')}>🌐 From URL</button>
            </div>
            {subTab === 'file' ? <IngestFile onIngested={onIngested} /> : <IngestUrl onIngested={onIngested} />}
        </div>
    )
}

function IngestFile({ onIngested }: { onIngested: () => void }) {
    const [file, setFile]       = useState<File|null>(null)
    const [loading, setLoading] = useState(false)
    const [result, setResult]   = useState<IngestResponse|null>(null)
    const [error, setError]     = useState('')
    const [dragging, setDragging] = useState(false)
    const inputRef = useRef<HTMLInputElement>(null)

    const submit = async () => {
        if (!file) return
        setLoading(true); setResult(null); setError('')
        try {
            const r = await ingestFile(file)
            setResult(r)
            onIngested()
        } catch(e: any) { setError(e.message) }
        finally { setLoading(false) }
    }

    return (
        <>
            <p style={{color:'var(--text-muted)',fontSize:13,marginBottom:14}}>
                Supported: <strong>audio/video</strong> (Whisper transcription), <strong>PDF</strong> (PDFBox), <strong>DOCX</strong> (Apache POI), <strong>plain text</strong>
            </p>

            <div
                className={`drop-zone ${dragging?'dragging':''}`}
                onDragOver={e=>{e.preventDefault();setDragging(true)}}
                onDragLeave={()=>setDragging(false)}
                onDrop={e=>{e.preventDefault();setDragging(false);if(e.dataTransfer.files[0])setFile(e.dataTransfer.files[0])}}
                onClick={()=>inputRef.current?.click()}
                style={{marginBottom:16}}
            >
                <div className="drop-zone-icon">{file?'📄':'📂'}</div>
                {file
                    ? <p><strong>{file.name}</strong> ({(file.size/1024/1024).toFixed(2)} MB)</p>
                    : <p>Drag & drop or <strong>click to browse</strong></p>
                }
                <input ref={inputRef} type="file" style={{display:'none'}}
                       onChange={e=>{ if(e.target.files?.[0]) setFile(e.target.files[0]) }} />
            </div>

            <button className="btn btn-primary btn-full" disabled={!file||loading} onClick={submit}>
                {loading ? <><span className="spinner"/>&nbsp;Ingesting… (audio/video may take a while)</> : '📥 Ingest File'}
            </button>

            {result && (
                <div className={`alert ${result.status==='success'?'alert-success':'alert-error'}`} style={{marginTop:12}}>
                    {result.status === 'success'
                        ? <>✅ <strong>{result.source}</strong> ingested — <strong>{result.charactersIndexed.toLocaleString()}</strong> characters indexed</>
                        : <>❌ {result.status}</>
                    }
                </div>
            )}
            {error && <div className="alert alert-error" style={{marginTop:12}}>❌ {error}</div>}
        </>
    )
}

function IngestUrl({ onIngested }: { onIngested: () => void }) {
    const [url, setUrl]         = useState('')
    const [loading, setLoading] = useState(false)
    const [result, setResult]   = useState<IngestResponse|null>(null)
    const [error, setError]     = useState('')

    const submit = async () => {
        if (!url.trim()) return
        setLoading(true); setResult(null); setError('')
        try {
            const r = await ingestUrl(url.trim())
            setResult(r)
            onIngested()
        } catch(e: any) { setError(e.message) }
        finally { setLoading(false) }
    }

    return (
        <>
            <p style={{color:'var(--text-muted)',fontSize:13,marginBottom:14}}>
                Paste a <strong>webpage URL</strong> (scraped via JSoup) or a <strong>YouTube link</strong> (downloaded via yt-dlp + Whisper transcription).
            </p>

            <div className="form-row">
                <label>URL</label>
                <input
                    type="url"
                    placeholder="https://en.wikipedia.org/wiki/... or https://youtube.com/watch?v=..."
                    value={url}
                    onChange={e=>setUrl(e.target.value)}
                    onKeyDown={e=>e.key==='Enter'&&submit()}
                />
            </div>

            <div style={{display:'flex',gap:8,marginBottom:4}}>
                <span className="badge badge-info">🌐 Webpage</span>
                <span className="badge badge-warning">▶️ YouTube (requires yt-dlp)</span>
            </div>

            <button className="btn btn-primary btn-full" style={{marginTop:12}} disabled={!url.trim()||loading} onClick={submit}>
                {loading ? <><span className="spinner"/>&nbsp;Fetching & ingesting…</> : '🌐 Ingest URL'}
            </button>

            {result && (
                <div className={`alert ${result.status==='success'?'alert-success':'alert-error'}`} style={{marginTop:12}}>
                    {result.status === 'success'
                        ? <>✅ <strong>{result.source}</strong> ingested — <strong>{result.charactersIndexed.toLocaleString()}</strong> characters indexed</>
                        : <>❌ {result.status}</>
                    }
                </div>
            )}
            {error && <div className="alert alert-error" style={{marginTop:12}}>❌ {error}</div>}
        </>
    )
}

// ── Chat Tab ──────────────────────────────────────────────────────────────────

function ChatTab({ contextLoaded }: { contextLoaded: boolean }) {
    const [messages, setMessages] = useState<ChatMessage[]>([])
    const [input, setInput]       = useState('')
    const [loading, setLoading]   = useState(false)
    const bottomRef = useRef<HTMLDivElement>(null)

    useEffect(() => {
        bottomRef.current?.scrollIntoView({ behavior: 'smooth' })
    }, [messages])

    const send = async () => {
        const q = input.trim()
        if (!q || loading) return
        setInput('')
        setMessages(prev => [...prev, { role: 'user', text: q }])
        setLoading(true)
        try {
            const r: AskResponse = await askQuestion(q)
            setMessages(prev => [...prev, { role: 'assistant', text: r.answer, modelUsed: r.modelUsed }])
        } catch(e: any) {
            setMessages(prev => [...prev, { role: 'assistant', text: `Error: ${e.message}`, modelUsed: 'error' }])
        } finally { setLoading(false) }
    }

    return (
        <div className="card">
            <div className="card-title">💬 Ask Questions</div>

            {!contextLoaded && (
                <div className="alert alert-info" style={{marginBottom:16}}>
                    ℹ️ No context loaded yet. Go to <strong>Ingest Content</strong> tab to add documents or URLs first.
                </div>
            )}

            {messages.length === 0 && contextLoaded && (
                <div style={{textAlign:'center',padding:'24px 0',color:'var(--text-muted)'}}>
                    <div style={{fontSize:32,marginBottom:8}}>🤖</div>
                    <p>Context loaded. Ask anything about your ingested documents.</p>
                </div>
            )}

            {messages.length > 0 && (
                <div className="chat-history">
                    {messages.map((m, i) => (
                        <div key={i} className={`chat-bubble ${m.role}`}>
                            <div>{m.text}</div>
                            {m.modelUsed && m.role === 'assistant' && (
                                <div className="model-tag">
                                    {m.modelUsed === 'error' ? '⚠️ error' : `🤖 ${m.modelUsed}`}
                                </div>
                            )}
                        </div>
                    ))}
                    {loading && (
                        <div className="chat-bubble assistant">
                            <span className="spinner" style={{borderTopColor:'var(--primary)',borderColor:'var(--border)'}} />
                            &nbsp; Thinking…
                        </div>
                    )}
                    <div ref={bottomRef} />
                </div>
            )}

            <div className="chat-input-row">
                <input
                    type="text"
                    placeholder={contextLoaded ? "Ask a question about your documents…" : "Ingest content first…"}
                    value={input}
                    disabled={!contextLoaded || loading}
                    onChange={e=>setInput(e.target.value)}
                    onKeyDown={e=>e.key==='Enter'&&send()}
                />
                <button className="btn btn-primary" disabled={!input.trim()||!contextLoaded||loading} onClick={send}>
                    {loading ? <span className="spinner"/> : 'Send'}
                </button>
            </div>

            {messages.length > 0 && (
                <button className="btn btn-outline btn-sm" style={{marginTop:10}} onClick={()=>setMessages([])}>
                    🗑️ Clear chat history
                </button>
            )}
        </div>
    )
}

// ── Context Management Tab ────────────────────────────────────────────────────

function ContextTab({ onCleared, sources }: { onCleared: () => void; sources: string[] }) {
    const [loading, setLoading] = useState(false)
    const [done, setDone]       = useState(false)

    const handleClear = async () => {
        if (!window.confirm('Clear all ingested context? This cannot be undone.')) return
        setLoading(true); setDone(false)
        try {
            await clearContext()
            setDone(true)
            onCleared()
        } catch {}
        finally { setLoading(false) }
    }

    return (
        <div className="card">
            <div className="card-title">⚙️ Context Management</div>

            <p style={{color:'var(--text-muted)',fontSize:13,marginBottom:16}}>
                All ingested context is stored <strong>in memory</strong> — it is lost when the Q&A service restarts.
            </p>

            {sources.length > 0 ? (
                <>
                    <label style={{marginBottom:10,display:'block'}}>Currently loaded sources ({sources.length})</label>
                    <ul className="file-list">
                        {sources.map(s => (
                            <li key={s} className="file-item">
                                <span className="file-item-name">📄 {s}</span>
                                <span className="badge badge-success">loaded</span>
                            </li>
                        ))}
                    </ul>
                </>
            ) : (
                <div style={{textAlign:'center',padding:'20px 0',color:'var(--text-muted)'}}>
                    <div style={{fontSize:28,marginBottom:8}}>📭</div>
                    <p>No context loaded.</p>
                </div>
            )}

            <hr className="divider" />

            <div style={{display:'flex',alignItems:'center',justifyContent:'space-between'}}>
                <div>
                    <div style={{fontWeight:600,marginBottom:4}}>Clear All Context</div>
                    <div style={{fontSize:12,color:'var(--text-muted)'}}>Removes all ingested documents and URLs from memory.</div>
                </div>
                <button className="btn btn-danger" disabled={sources.length===0||loading} onClick={handleClear}>
                    {loading ? <><span className="spinner"/>&nbsp;Clearing…</> : '🗑️ Clear Context'}
                </button>
            </div>

            {done && <div className="alert alert-success" style={{marginTop:14}}>✅ All context cleared.</div>}
        </div>
    )
}