package dentex.youtube.downloader.utils;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;

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
	
	public static String decipher(String S, String function) { // TODO fetch the function from http://userscripts.org/scripts/review/25105 for auto update!!!
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
