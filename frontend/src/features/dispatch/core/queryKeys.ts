export const dispatchKeys = {
  orders: (runDate: string, depot: string) => ['dispatch-orders', runDate, depot] as const,
  fleet: (runDate: string, depot: string) => ['dispatch-fleet', runDate, depot] as const,
  outlets: (depot: string) => ['dispatch-outlets', depot] as const,
}
