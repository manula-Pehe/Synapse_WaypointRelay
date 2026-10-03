import DispatcherLayout from './components/DispatcherLayout';
import OrderQueue from './components/OrderQueue';

export function App() {
  return (
    <DispatcherLayout>
      <OrderQueue />
    </DispatcherLayout>
  );
}

export default App;
