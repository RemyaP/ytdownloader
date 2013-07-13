package dentex.youtube.downloader.utils;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;

import dentex.youtube.downloader.ShareActivity;

public class RhinoRunner {
	
	static String DEBUG_TAG = "RhinoRunner";
	
	/*
	 * method runner() adapted from Stack Overflow:
	 * http://stackoverflow.com/questions/3995897/rhino-how-to-call-js-function-from-java/3996115#3996115
	 * 
	 * Q:http://stackoverflow.com/users/391441/instantsetsuna
	 * A:http://stackoverflow.com/users/72673/maurice-perry
	 */
	
	/*
     * "function decryptSignature(sig)" from the Javascript Greasemonkey script 
     * http://userscripts.org/scripts/show/25105 (released under the MIT License)
     * by Gantt: http://userscripts.org/users/gantt
     */
	
	public static String decipher(String S) { // TODO fetch the function from http://userscripts.org/scripts/review/25105 for auto update!!!
		//String function = "function decryptSignature(sig) { function swap(a,b){var c=a[0];a[0]=a[b%a.length];a[b]=c;return a}; if (sig.length==88) { var sigA=sig.split(''); sigA=sigA.slice(2);sigA=swap(sigA,1);sigA=swap(sigA,10); sigA=sigA.reverse();sigA=sigA.slice(2);sigA=swap(sigA,23); sigA=sigA.slice(3);sigA=swap(sigA,15);sigA=swap(sigA,34); sig=sigA.join(''); } else if (sig.length==87) { var sigA=sig.substr(44,40).split('').reverse().join(''); var sigB=sig.substr(3,40).split('').reverse().join(''); sig=sigA.substr(21,1)+sigA.substr(1,20)+sigA.substr(0,1)+sigB.substr(22,9)+ sig.substr(0,1)+sigA.substr(32,8)+sig.substr(43,1)+sigB; } else if (sig.length==86) { var sigA=sig.substr(2,40); var sigB=sig.substr(43,40); sig=sigA+sig.substr(42,1)+sigB.substr(0,20)+sigB.substr(39,1)+sigB.substr(21,18)+sigB.substr(20,1); } else if (sig.length==85) { var sigA=sig.substr(44,40).split('').reverse().join(''); var sigB=sig.substr(3,40).split('').reverse().join(''); sig=sigA.substr(7,1)+sigA.substr(1,6)+sigA.substr(0,1)+sigA.substr(8,15)+sig.substr(0,1)+ sigA.substr(24,9)+sig.substr(1,1)+sigA.substr(34,6)+sig.substr(43,1)+sigB; } else if (sig.length==84) { var sigA=sig.substr(44,40).split('').reverse().join(''); var sigB=sig.substr(3,40).split('').reverse().join(''); sig=sigA+sig.substr(43,1)+sigB.substr(0,6)+sig.substr(2,1)+sigB.substr(7,9)+ sigB.substr(39,1)+sigB.substr(17,22)+sigB.substr(16,1); } else if (sig.length==83) { var sigA=sig.substr(2,40); var sigB=sig.substr(43,40); sig=sigA.substr(4,1)+sigA.substr(1,3)+sigA.substr(31,1)+sigA.substr(5,17)+ sig.substr(0,1)+sigA.substr(23,8)+sigB.substr(10,1)+sigA.substr(32,8)+ sig.substr(42,1)+sigB.substr(0,10)+sigA.substr(22,1)+sigB.substr(11,29); } else if (sig.length==82) { var sigA=sig.substr(34,48).split('').reverse().join(''); var sigB=sig.substr(0,33).split('').reverse().join(''); sig=sigA.substr(45,1)+sigA.substr(2,12)+sigA.substr(0,1)+sigA.substr(15,26)+ sig.substr(33,1)+sigA.substr(42,1)+sigA.substr(43,1)+sigA.substr(44,1)+ sigA.substr(41,1)+sigA.substr(46,1)+sigB.substr(32,1)+sigA.substr(14,1)+ sigB.substr(0,32)+sigA.substr(47,1); } return sig; }"; 
		String function = ShareActivity.ganttFunction;
		Context rhino = Context.enter();
		rhino.setOptimizationLevel(-1);
		try {
		    ScriptableObject scope = rhino.initStandardObjects();
		    Scriptable that = rhino.newObject(scope);
		    Function fct = rhino.compileFunction(scope, function, "script", 1, null);
		    
		    Object result = fct.call(rhino, scope, that, new Object[] {S});
		    
		    return (String) Context.jsToJava(result, String.class);
		    
		} finally {
		    Context.exit();
		}
	}
}
