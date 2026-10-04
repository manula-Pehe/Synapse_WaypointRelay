/**
 * The signature pad hands back a data URL; the file endpoint wants bytes.
 *
 * PNG because that is what `canvas.toDataURL` produces, and `kind: 'signature'` is what the
 * endpoint stores it as.
 */
export function dataUrlToFile(dataUrl: string): File {
  const [header, base64] = dataUrl.split(',')
  const mime = /:(.*?);/.exec(header)?.[1] ?? 'image/png'
  const binary = atob(base64 ?? '')
  const bytes = new Uint8Array(binary.length)
  for (let index = 0; index < binary.length; index += 1) bytes[index] = binary.charCodeAt(index)
  return new File([bytes], 'signature.png', { type: mime })
}
