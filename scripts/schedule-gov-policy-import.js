#!/usr/bin/env node

const { spawn } = require('child_process')
const path = require('path')

const ONE_DAY_MS = 24 * 60 * 60 * 1000
const DEFAULT_KEYWORD = '毕业生,创业,大学生,就业'
const DEFAULT_OUTPUT = path.join('scripts', 'generated', 'graduate_policies.sql')

function getArg(name, fallback) {
  const prefix = `--${name}=`
  const found = process.argv.find((arg) => arg.startsWith(prefix))
  return found ? found.slice(prefix.length) : fallback
}

function timestamp() {
  return new Date().toISOString().replace('T', ' ').slice(0, 19)
}

function runCommand(command, args, options = {}) {
  return new Promise((resolve, reject) => {
    const child = spawn(command, args, {
      stdio: 'inherit',
      shell: process.platform === 'win32',
      ...options
    })

    child.on('error', reject)
    child.on('exit', (code) => {
      if (code === 0) {
        resolve()
      } else {
        reject(new Error(`${command} exited with code ${code}`))
      }
    })
  })
}

async function runImporter() {
  const keyword = getArg('keyword', process.env.POLICY_IMPORT_KEYWORD || DEFAULT_KEYWORD)
  const pages = getArg('pages', process.env.POLICY_IMPORT_PAGES || '2')
  const output = getArg('output', process.env.POLICY_IMPORT_OUTPUT || DEFAULT_OUTPUT)

  console.log(`[${timestamp()}] starting gov.cn policy import`)
  await runCommand(process.execPath, [
    path.join('scripts', 'import-gov-graduate-policies.js'),
    `--keyword=${keyword}`,
    `--pages=${pages}`,
    `--output=${output}`
  ])
  console.log(`[${timestamp()}] policy import finished`)
}

async function runLoop() {
  const intervalMs = Number(getArg('interval-ms', process.env.POLICY_IMPORT_INTERVAL_MS || ONE_DAY_MS))

  if (!Number.isFinite(intervalMs) || intervalMs <= 0) {
    throw new Error('interval-ms must be a positive number')
  }

  console.log(`[${timestamp()}] scheduler started, interval: ${intervalMs} ms`)

  while (true) {
    try {
      await runImporter()
    } catch (err) {
      console.error(`[${timestamp()}] policy import failed: ${err.message}`)
    }

    console.log(`[${timestamp()}] next import in ${Math.round(intervalMs / 1000 / 60)} minutes`)
    await new Promise((resolve) => setTimeout(resolve, intervalMs))
  }
}

runLoop().catch((err) => {
  console.error(`[${timestamp()}] scheduler stopped: ${err.message}`)
  process.exit(1)
})
