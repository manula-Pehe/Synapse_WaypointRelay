export type IconName = 'check' | 'warning' | 'truck' | 'bell' | 'close' | 'search' | 'arrow'
const paths: Record<IconName, string> = {
  check: 'M20 6 9 17l-5-5',
  warning: 'M12 9v4m0 4h.01M10.3 3.4 2 18a2 2 0 0 0 1.7 3h16.6A2 2 0 0 0 22 18L13.7 3.4a2 2 0 0 0-3.4 0Z',
  truck: 'M3 6h12v10H3zM15 9h4l3 3v4h-7M7 20a2 2 0 1 0 0-4 2 2 0 0 0 0 4Zm12 0a2 2 0 1 0 0-4 2 2 0 0 0 0 4Z',
  bell: 'M18 8a6 6 0 0 0-12 0c0 7-3 9-3 9h18s-3-2-3-9m-8 4h4m-3 9h2',
  close: 'M5 5l14 14M19 5 5 19',
  search: 'M11 19a8 8 0 1 0 0-16 8 8 0 0 0 0 16Zm6-2 5 5',
  arrow: 'M5 12h14m-6-6 6 6-6 6',
}
export function Icon({ name, className = 'size-5' }: { name: IconName; className?: string }) { return <svg className={className} viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true"><path d={paths[name]} /></svg> }
