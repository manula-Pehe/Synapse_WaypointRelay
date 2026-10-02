import { registerHooks } from 'node:module'
// Resolve the extensionless TypeScript imports used by Vite for Node's test runner.
registerHooks({
  resolve(specifier, context, nextResolve) {
    if (specifier.startsWith('.') && !/\.[a-z]+$/.test(specifier)) {
      for (const suffix of ['.ts', '/index.ts']) {
        try {
          return nextResolve(specifier + suffix, context)
        } catch (error) {
          if (error.code !== 'ERR_MODULE_NOT_FOUND' && error.code !== 'ENOTDIR') throw error
        }
      }
    }
    return nextResolve(specifier, context)
  },
})
