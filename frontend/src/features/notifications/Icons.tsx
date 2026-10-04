export type IconName = 'home' | 'orders' | 'truck' | 'warning' | 'user' | 'more' | 'box' | 'check' | 'info' | 'critical' | 'bell' | 'close' | 'arrow'
export function NoticeIcon({ name, className = 'size-5' }: { name: IconName; className?: string }) {
  const paths: Record<IconName, React.ReactNode> = {
    home: <><path d="m3 10 9-7 9 7v11h-6v-7H9v7H3Z" /></>,
    orders: <><path d="M8 5h13M8 12h13M8 19h13M3 5h.01M3 12h.01M3 19h.01" /></>,
    truck: <><path d="M2 5h12v12H2ZM14 9h4l4 4v4h-8" /><circle cx="6" cy="18" r="2" /><circle cx="18" cy="18" r="2" /></>,
    warning: <><path d="m12 3 10 18H2ZM12 9v5M12 17h.01" /></>,
    user: <><circle cx="12" cy="7" r="4" /><path d="M4 22v-2a8 8 0 0 1 16 0v2" /></>,
    more: <path d="M4 5h16M4 12h16M4 19h16" />,
    box: <><path d="m12 3 9 4.5v9L12 21l-9-4.5v-9ZM3 7.5l9 5 9-5M12 12.5V21M7.5 5.25l9 5v4" /></>,
    check: <><circle cx="12" cy="12" r="9" /><path d="m8 12 3 3 5-6" /></>,
    info: <><circle cx="12" cy="12" r="9" /><path d="M12 11v6M12 7h.01" /></>,
    critical: <><path d="m8 2-6 6v8l6 6h8l6-6V8l-6-6ZM8 8l8 8M16 8l-8 8" /></>,
    bell: <path d="M18 8a6 6 0 0 0-12 0c0 7-3 7-3 9h18c0-2-3-2-3-9M10 21h4" />,
    close: <path d="m6 6 12 12M18 6 6 18" />,
    arrow: <path d="m9 5 7 7-7 7" />,
  }
  return <svg aria-hidden="true" viewBox="0 0 24 24" className={className} fill="none" stroke="currentColor" strokeWidth="1.7" strokeLinecap="round" strokeLinejoin="round">{paths[name]}</svg>
}
