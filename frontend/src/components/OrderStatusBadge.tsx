import { Badge } from '../ui/components'
import { STATUS_STYLES } from './orderStatus'

export function OrderStatusBadge({ status }: { status: string }) {
  const style = STATUS_STYLES[status] ?? { label: status, status: 'offline' as const, icon: '•' }
  return <Badge status={style.status} size="s" icon={style.icon}>{style.label}</Badge>
}
