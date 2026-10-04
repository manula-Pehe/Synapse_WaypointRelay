import { useState } from 'react'
import { AppBar, Badge, Button, Card, Checkbox, Dialog, EmptyState, IconButton, Input, Select, Sheet, SideNav, Spinner, Stepper, Switch, Tabs, ThemeSwitch, Toast } from './components'
import { Icon, type IconName } from './icons'

const guideIcons: IconName[] = [
  'auto', 'back', 'bell', 'box', 'camera', 'chart', 'check', 'chev', 'clock', 'cloudoff',
  'crit', 'defer', 'dot', 'drag', 'globe', 'grid', 'home', 'info', 'list', 'lock',
  'map', 'menu', 'minus', 'moon', 'pen', 'phone', 'plus', 'print', 'pulse', 'ring',
  'send', 'snow', 'split', 'sq', 'sqc', 'sun', 'sync', 'truck', 'undo', 'user',
  'wand', 'warn', 'why', 'wrench', 'x',
]

const buttonTones = ['primary', 'secondary', 'neutral', 'outline', 'tonal', 'critical'] as const
const buttonSizes = ['s', 'm', 'l', 'xl'] as const
const badgeSamples = [
  { status: 'way', label: 'On the way', icon: 'truck' },
  { status: 'delivered', label: 'Delivered', icon: 'check' },
  { status: 'risk', label: 'Late risk', icon: 'warn' },
  { status: 'failed', label: 'Failed', icon: 'crit' },
  { status: 'deferred', label: 'Deferred', icon: 'defer' },
  { status: 'chilled', label: 'Chilled', icon: 'snow' },
  { status: 'offline', label: 'Offline', icon: 'cloudoff' },
] as const

export default function UIShowcase() {
  const [count, setCount] = useState(2)
  const [checked, setChecked] = useState(false)
  const [active, setActive] = useState('one')
  const [sheet, setSheet] = useState(false)
  const [dialog, setDialog] = useState(false)
  const [toast, setToast] = useState(false)
  return <div className="min-h-screen bg-canvas text-ink"><AppBar title="Waypoint Relay · Design system" action={<ThemeSwitch />} /><main className="mx-auto max-w-6xl space-y-6 p-6">
    <Card><h2 className="mb-3 text-xl font-bold">Icons · 24 px</h2><div className="grid grid-cols-5 gap-4 sm:grid-cols-9 lg:grid-cols-15">{guideIcons.map(name => <div key={name} className="flex flex-col items-center gap-1 text-center"><Icon name={name} /><span className="text-xs text-ink-2">{name}</span></div>)}</div></Card>
    <Card><h2 className="mb-3 text-xl font-bold">Button · type × size</h2><div className="space-y-4">{buttonTones.map(tone => <div key={tone} className="flex flex-wrap items-center gap-3">{buttonSizes.map(size => <Button key={size} tone={tone} size={size}><Icon name="check" className="size-4" />{tone}</Button>)}</div>)}</div><h3 className="mt-6 mb-3 font-semibold">Icon buttons</h3><div className="flex flex-wrap items-center gap-3">{(['neutral', 'outline', 'primary'] as const).map(tone => buttonSizes.map(size => <IconButton key={`${tone}-${size}`} label={`${tone} ${size}`} tone={tone} size={size}><Icon name="check" /></IconButton>))}</div></Card>
    <Card><h2 className="mb-3 text-xl font-bold">Badge · status × size</h2><div className="space-y-3">{badgeSamples.map(sample => <div key={sample.status} className="flex flex-wrap items-center gap-3">{(['s', 'm', 'l'] as const).map(size => <Badge key={size} status={sample.status} size={size} icon={<Icon name={sample.icon} className="size-4" />}>{sample.label}</Badge>)}</div>)}</div></Card>
    <Card><h2 className="mb-3 text-xl font-bold">Inputs and selection</h2><div className="grid gap-4 sm:grid-cols-2"><Input id="demo-input" label="Order reference" placeholder="S1-001" /><Select id="demo-select" label="Depot"><option>Peliyagoda</option><option>Kandy</option></Select></div><div className="mt-4 flex flex-wrap items-center gap-5"><Stepper label="Cases" value={count} onChange={setCount} /><Checkbox label="Confirm order" checked={checked} onChange={event => setChecked(event.target.checked)} /><Switch label="Auto-confirm" checked={checked} onChange={setChecked} /></div><div className="mt-4"><Tabs label="Demo tabs" items={[{ id: 'one', label: 'Orders' }, { id: 'two', label: 'Fleet' }]} active={active} onChange={setActive} /></div></Card>
    <Card><h2 className="mb-3 text-xl font-bold">Overlays and feedback</h2><div className="flex flex-wrap gap-3"><Button onClick={() => setSheet(true)}>Open sheet</Button><Button tone="secondary" onClick={() => setDialog(true)}>Open dialog</Button><Button tone="tonal" onClick={() => setToast(true)}>Show toast</Button></div><div className="mt-4"><Spinner /></div></Card>
    <div className="grid gap-4 sm:grid-cols-2"><EmptyState title="Nothing to show" description="Items will appear here when available." action={<Button size="s">Refresh</Button>} /><Card><h2 className="mb-2 font-bold">Side navigation</h2><SideNav items={[{ id: 'one', label: 'Orders' }, { id: 'two', label: 'Fleet' }]} active={active} onChange={setActive} /></Card></div>
  </main><Sheet open={sheet} title="Side sheet" onClose={() => setSheet(false)}>Sheet content</Sheet><Dialog open={dialog} title="Dialog" onClose={() => setDialog(false)}>Dialog content</Dialog>{toast && <Toast message="Saved" tone="success" onDismiss={() => setToast(false)} />}</div>
}
