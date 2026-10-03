import DispatcherLayout from './components/DispatcherLayout';

export function App() {
  return (
    <DispatcherLayout>
      <div className="flex min-h-[400px] flex-col items-center justify-center rounded-2xl border-2 border-dashed border-slate-200 bg-white p-12 text-center shadow-2xs">
        <div className="flex h-12 w-12 items-center justify-center rounded-full bg-blue-50 text-blue-600 mb-3">
          <svg className="h-6 w-6" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
            <path strokeLinecap="round" strokeLinejoin="round" d="M19 11H5m14 0a2 2 0 012 2v6a2 2 0 01-2 2H5a2 2 0 01-2-2v-6a2 2 0 012-2m14 0V9a2 2 0 00-2-2M5 11V9a2 2 0 012-2m0 0V5a2 2 0 012-2h6a2 2 0 012 2v2M7 7h10" />
          </svg>
        </div>
        <h3 className="text-base font-semibold text-slate-800">
          Dispatcher Main Content Area
        </h3>
        <p className="mt-1 text-sm text-slate-500 max-w-sm">
          Content rendered inside <code className="rounded bg-slate-100 px-1.5 py-0.5 text-xs text-slate-700">DispatcherLayout</code> (Order Queue, Fleet, Plan, etc.).
        </p>
      </div>
    </DispatcherLayout>
  );
}

export default App;
