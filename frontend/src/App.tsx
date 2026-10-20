import React, { useState } from 'react'
import UploadPage   from './pages/UploadPage'
import ConvertPage  from './pages/ConvertPage'
import DownloadPage from './pages/DownloadPage'
import QaPage       from './pages/QaPage'

type Page = 'upload' | 'convert' | 'download' | 'qa'

const NAV: { id: Page; icon: string; label: string; section?: string }[] = [
    { id: 'upload',   icon: '📤', label: 'Upload',    section: 'Media Pipeline' },
    { id: 'convert',  icon: '🔄', label: 'Convert'   },
    { id: 'download', icon: '⬇️', label: 'Download'  },
    { id: 'qa',       icon: '🤖', label: 'Q&A',       section: 'Intelligence' },
]

export default function App() {
    const [page, setPage] = useState<Page>('upload')

    const renderPage = () => {
        switch(page) {
            case 'upload':   return <UploadPage />
            case 'convert':  return <ConvertPage />
            case 'download': return <DownloadPage />
            case 'qa':       return <QaPage />
        }
    }

    return (
        <div className="layout">
            {/* Sidebar */}
            <aside className="sidebar">
                <div className="sidebar-logo">
                    <h1>MediaSense</h1>
                    <span>Media Processing Platform</span>
                </div>
                <nav className="sidebar-nav">
                    {NAV.map(item => (
                        <React.Fragment key={item.id}>
                            {item.section && (
                                <div className="nav-section-label">{item.section}</div>
                            )}
                            <div
                                className={`nav-item ${page === item.id ? 'active' : ''}`}
                                onClick={() => setPage(item.id)}
                            >
                                <span className="nav-icon">{item.icon}</span>
                                {item.label}
                            </div>
                        </React.Fragment>
                    ))}
                </nav>

                {/* Service status indicators */}
                <div style={{padding:'16px 20px',borderTop:'1px solid var(--border)'}}>
                    <div style={{fontSize:10,fontWeight:600,textTransform:'uppercase',letterSpacing:1,color:'var(--text-muted)',marginBottom:10}}>Services</div>
                    {[
                        { label: 'Upload',     port: 8081 },
                        { label: 'Conversion', port: 8080 },
                        { label: 'Download',   port: 8082 },
                        { label: 'Q&A',        port: 8083 },
                    ].map(s => (
                        <div key={s.label} style={{display:'flex',justifyContent:'space-between',alignItems:'center',marginBottom:6}}>
                            <span style={{fontSize:12,color:'var(--text-muted)'}}>{s.label}</span>
                            <span style={{fontSize:11,color:'var(--text-muted)',background:'var(--bg3)',padding:'1px 7px',borderRadius:10}}>:{s.port}</span>
                        </div>
                    ))}
                </div>
            </aside>

            {/* Main content */}
            <main className="main-content">
                {renderPage()}
            </main>
        </div>
    )
}