import { useEffect, useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import { apiBlob } from '../../lib/api'

const API_PREFIX = '/api/'

interface ProofImageProps {
  /** File link as returned by the API, e.g. `/api/files/{id}`. */
  url: string
  alt: string
}

function useObjectUrl(blob: Blob | undefined): string | undefined {
  const [objectUrl, setObjectUrl] = useState<string>()
  useEffect(() => {
    if (!blob) return
    const created = URL.createObjectURL(blob)
    // An object URL is an external resource that must be created and revoked with the component.
    // eslint-disable-next-line react-hooks/set-state-in-effect
    setObjectUrl(created)
    return () => URL.revokeObjectURL(created)
  }, [blob])
  return objectUrl
}

/** Proof of delivery picture, fetched through the API client so the request carries the sign-in token. */
export function ProofImage({ url, alt }: ProofImageProps) {
  const path = url.startsWith(API_PREFIX) ? url.slice(API_PREFIX.length) : url
  const query = useQuery({ queryKey: ['store', 'proof-file', path], queryFn: () => apiBlob(path), staleTime: Infinity, retry: 1 })
  const objectUrl = useObjectUrl(query.data)
  if (query.isError) return <span role="alert" className="text-danger">Could not load the photo</span>
  if (!objectUrl) return <span role="status" className="text-muted">Loading…</span>
  return <a href={objectUrl} target="_blank" rel="noreferrer" title="Open larger"><img src={objectUrl} alt={alt} className="max-h-48 rounded-lg border border-border object-contain" /></a>
}
