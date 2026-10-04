import { LoaderLayout } from './LoaderLayout'

export function LoaderHome() {
  return <LoaderLayout><section className="rounded-xl border border-line bg-surface p-6"><h1 className="text-2xl font-bold">Trips to load</h1><p className="mt-3 text-muted">Loading lists will appear here.</p></section></LoaderLayout>
}
