#! /usr/bin/env node


const fs = require('fs');
const jsdom = require ('jsdom');
const {JSDOM} = jsdom;

const { window } = new JSDOM("", { runScripts: "dangerously" });

global.window = window;
require('raf-polyfill')

specljsRunner = "\nspecljRunner = function () {\n" +
    "  speclj.run.standard.armed = true;\n" +
    "  return speclj.run.standard.run_specs(\n" +
    "     cljs.core.keyword(\"color\"), true,\n" +
    "     cljs.core.keyword(\"reporters\"), [\"documentation\"]\n" +
    "  );\n" +
    "}\n;"

function loadAndAppend (fname, w){
    var text =  fs.readFileSync(fname, { encoding: "utf-8" });
    text = text + specljsRunner;
    var scriptEl = w.document.createElement("script");
    scriptEl.textContent = text;
    w.document.body.appendChild(scriptEl);
}

// Args: 0 = node, 1 = this file, 2 = first command line arg
loadAndAppend(process.argv[2], window);

var result = window.eval("specljRunner()");

process.exit(result);