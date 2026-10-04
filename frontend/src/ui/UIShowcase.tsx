import { useState } from 'react'
import { AppBar, Badge, Button, Card, Checkbox, Dialog, EmptyState, IconButton, Input, Select, Sheet, SideNav, Spinner, Stepper, Switch, Tabs, ThemeSwitch, Toast } from './components'
import { Icon } from './icons'

export default function UIShowcase() {
  const [count, setCount] = useState(2)
  const [checked, setChecked] = useState(false)
  const [active, setActive] = useState('one')
  const [sheet, setSheet] = useState(false)
  const [dialog, setDialog] = useState(false)
  const [toast, setToast] = useState(false)
  return <div className="min-h-screen bg-canvas text-ink"><AppBar title="Waypoint Relay · UI kit" action={<ThemeSwitch />} /><main className="mx-auto max-w-6xl space-y-6 p-6">
    <Card><h2 className="mb-3 text-xl font-bold">Buttons</h2><div className="flex flex-wrap items-center gap-3">{(['primary', 'secondary', 'neutral', 'outline', 'tonal', 'critical'] as const).map(tone => <Button key={tone} tone={tone}>{tone}</Button>)}</div><div className="mt-3 flex flex-wrap items-center gap-3">{(['s', 'm', 'l', 'xl'] as const).map(size => <Button key={size} size={size}>{size.toUpperCase()}</Button>)}<IconButton label="Search"><Icon name="search" /></IconButton></div></Card>
    <Card><h2 className="mb-3 text-xl font-bold">Status</h2><div className="flex flex-wrap gap-2"><Badge tone="info" icon={<Icon name="truck" />}>On the way</Badge><Badge tone="success" icon={<Icon name="check" />}>Delivered</Badge><Badge tone="warning" icon={<Icon name="warning" />}>Late risk</Badge><Badge tone="danger" icon={<Icon name="warning" />}>Failed</Badge><Badge tone="neutral" icon={<Icon name="arrow" />}>Deferred</Badge><Badge tone="info" icon="❄">Chilled</Badge><Badge tone="neutral" icon="◌">Offline</Badge></div></Card>
    <Card><h2 className="mb-3 text-xl font-bold">Inputs and selection</h2><div className="grid gap-4 sm:grid-cols-2"><Input id="demo-input" label="Order reference" placeholder="S1-001" /><Select id="demo-select" label="Depot"><option>Peliyagoda</option><option>Kandy</option></Select></div><div className="mt-4 flex flex-wrap items-center gap-5"><Stepper label="Cases" value={count} onChange={setCount} /><Checkbox label="Confirm order" checked={checked} onChange={event => setChecked(event.target.checked)} /><Switch label="Auto-confirm" checked={checked} onChange={setChecked} /></div><div className="mt-4"><Tabs label="Demo tabs" items={[{ id: 'one', label: 'Orders' }, { id: 'two', label: 'Fleet' }]} active={active} onChange={setActive} /></div></Card>
    <Card><h2 className="mb-3 text-xl font-bold">Overlays and feedback</h2><div className="flex flex-wrap gap-3"><Button onClick={() => setSheet(true)}>Open sheet</Button><Button tone="secondary" onClick={() => setDialog(true)}>Open dialog</Button><Button tone="tonal" onClick={() => setToast(true)}>Show toast</Button></div><div className="mt-4"><Spinner /></div></Card>
    <div className="grid gap-4 sm:grid-cols-2"><EmptyState title="Nothing to show" description="Items will appear here when available." action={<Button size="s">Refresh</Button>} /><Card><h2 className="mb-2 font-bold">Side navigation</h2><SideNav items={[{ id: 'one', label: 'Orders' }, { id: 'two', label: 'Fleet' }]} active={active} onChange={setActive} /></Card></div>
  </main><Sheet open={sheet} title="Side sheet" onClose={() => setSheet(false)}>Sheet content</Sheet><Dialog open={dialog} title="Dialog" onClose={() => setDialog(false)}>Dialog content</Dialog>{toast && <Toast message="Saved" tone="success" onDismiss={() => setToast(false)} />}</div>
}
