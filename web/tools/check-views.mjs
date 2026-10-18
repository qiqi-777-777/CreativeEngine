import { compile } from '@vue/compiler-dom'
import { views } from '../src/generated/views.js'

let bad = 0
for (const [p, v] of Object.entries(views)) {
  try {
    const { code } = compile(v.template, { mode: 'function', prefixIdentifiers: true, onError: (e) => { throw e } })
    // eslint-disable-next-line no-new-func
    new Function('Vue', code)
  } catch (e) {
    bad++
    console.log('FAIL', p, '=>', e.message)
    const idx = String(e.message).match(/\((\d+):(\d+)\)/)
    if (idx) {
      const line = Number(idx[1])
      const lines = v.template.split('\n')
      console.log('   line', line, ':', (lines[line - 1] || '').slice(0, 300))
    }
  }
}
console.log('checked', Object.keys(views).length, 'bad', bad)
