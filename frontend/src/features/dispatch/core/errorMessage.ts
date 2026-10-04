export const errorMessage = (error: unknown): string => error instanceof Error ? error.message : 'Something went wrong. Please try again.'
