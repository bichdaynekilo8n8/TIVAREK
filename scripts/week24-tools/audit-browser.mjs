// Read-only UI audit. No credentials, tokens or customer data are persisted.
import { chromium } from 'playwright';
import { createRequire } from 'node:module';
import { mkdir, writeFile } from 'node:fs/promises';
const require=createRequire(import.meta.url);
const base=(process.env.BASE_URL||'http://127.0.0.1:8080').replace(/\/$/,'');
const detail=process.env.DETAIL_PATH;
const paths=['/','/san-pham','/sua-chua','/gio-hang','/thanh-toan','/tra-cuu-sua-chua','/tra-cuu-bao-hanh',...(detail?[detail]:[])];
const browser=await chromium.launch({headless:true,executablePath:process.env.CHROME_PATH||undefined,args:process.env.AUDIT_NO_SANDBOX==='1'?['--no-sandbox']:[]});const results=[];
try {
 for(const width of [320,390,768,1440]) for(const path of paths) {
  const page=await browser.newPage({viewport:{width,height:900},reducedMotion:'reduce'});
  const errors=[];page.on('pageerror',e=>errors.push(e.message));
  page.on('console',m=>{if(m.type()==='error')errors.push(m.text())});
  const response=await page.goto(base+path,{waitUntil:'networkidle'});
  await page.addScriptTag({path:require.resolve('axe-core/axe.min.js')});
  const a11y=await page.evaluate(async()=>{
   const r=await axe.run(document,{runOnly:{type:'tag',values:['wcag2a','wcag2aa','wcag21a','wcag21aa','wcag22aa']}});
   return {violations:r.violations.map(v=>({id:v.id,impact:v.impact,help:v.help,nodes:v.nodes.map(n=>n.target)})),incomplete:r.incomplete.map(v=>({id:v.id,nodes:v.nodes.length}))};
  });
  const overflow=await page.evaluate(()=>document.documentElement.scrollWidth>innerWidth+1);
  // Skip link must be the first keyboard stop and transfer focus to the main landmark.
  await page.keyboard.press('Tab');
  const firstFocus=await page.evaluate(()=>document.activeElement?.getAttribute('href'));
  if(firstFocus==='#main-content') await page.keyboard.press('Enter');
  const mainFocus=await page.evaluate(()=>document.activeElement?.id==='main-content');
  const row={path,width,status:response.status(),overflow,errors,firstFocus,mainFocus,...a11y};results.push(row);
  console.log(`${response.status()} ${width}px ${path}: overflow=${overflow}, a11y=${a11y.violations.length}, js=${errors.length}`);
  await page.close();
 }
} finally { await browser.close(); }
await mkdir('results',{recursive:true});await writeFile('results/browser.json',JSON.stringify(results,null,2));
if(results.some(r=>r.status!==200||r.overflow||r.errors.length||r.violations.length||!r.mainFocus)) process.exitCode=1;
if(!detail) console.log('PENDING: set DETAIL_PATH to an actual published product URL to audit its detail page.');
