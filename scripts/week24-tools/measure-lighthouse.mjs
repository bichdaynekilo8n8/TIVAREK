// Three cold-load Lighthouse runs per page and device. Use the same machine/data before and after.
import lighthouse from 'lighthouse';
import desktopConfig from 'lighthouse/core/config/desktop-config.js';
import { launch } from 'chrome-launcher';
import { mkdir,writeFile } from 'node:fs/promises';
const base=(process.env.BASE_URL||'http://127.0.0.1:8080').replace(/\/$/,'');
const stage=process.env.STAGE||'after';
if(!['before','after'].includes(stage)) throw Error('STAGE must be before or after');
const paths=['/','/san-pham','/sua-chua'];
if(process.env.DETAIL_PATH) paths.push(process.env.DETAIL_PATH);
else console.log('PENDING: DETAIL_PATH is required for product-detail performance acceptance.');
await mkdir('results',{recursive:true});const rows=[];
for(const mode of ['mobile','desktop']) for(let i=0;i<paths.length;i++) for(let run=1;run<=3;run++) {
 const chrome=await launch({chromePath:process.env.CHROME_PATH,chromeFlags:['--headless','--disable-gpu',...(process.env.AUDIT_NO_SANDBOX==='1'?['--no-sandbox']:[])]});
 try {
  const result=await lighthouse(base+paths[i],{port:chrome.port,output:'json',logLevel:'error',onlyCategories:['performance']},mode==='desktop'?desktopConfig:undefined);
  const lhr=result.lhr;if(lhr.runtimeError) throw Error(lhr.runtimeError.message);
  const a=lhr.audits;const row={stage,path:paths[i],mode,run,performance:lhr.categories.performance.score*100,LCP:a['largest-contentful-paint']?.numericValue,CLS:a['cumulative-layout-shift']?.numericValue,TBT:a['total-blocking-time']?.numericValue,bytes:a['total-byte-weight']?.numericValue,requests:a['network-requests']?.details?.items?.length,serverResponseMs:a['server-response-time']?.numericValue,INP:null};
  rows.push(row);await writeFile(`results/lighthouse-${stage}.json`,JSON.stringify(rows,null,2));await writeFile(`results/${stage}-${mode}-${i}-${run}.json`,result.report);console.log(row);
 } finally {await chrome.kill();}
}
await writeFile(`results/lighthouse-${stage}.json`,JSON.stringify(rows,null,2));
console.log('INP requires real interactions / field data. TBT is a lab diagnostic, not an INP measurement.');
