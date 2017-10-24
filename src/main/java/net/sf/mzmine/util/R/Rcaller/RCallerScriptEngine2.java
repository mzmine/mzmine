package net.sf.mzmine.util.R.Rcaller;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;

import javax.script.Bindings;
import javax.script.Invocable;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineFactory;
import javax.script.ScriptException;
import javax.script.SimpleBindings;

import com.github.rcaller.EventHandler;
import com.github.rcaller.rstuff.RCaller;
import com.github.rcaller.rstuff.RCode;
import com.github.rcaller.rstuff.ROutputParser;
import com.github.rcaller.scriptengine.NamedArgument;
import com.github.rcaller.scriptengine.RCallerScriptEngineFactory;
import com.github.rcaller.util.RCodeUtils;

public class RCallerScriptEngine2 implements ScriptEngine, EventHandler, Invocable {

    final private RCaller rcaller;
    private final RCode rcode;
    private ROutputParser parser;
    private Bindings bindings;
    private ScriptContext context;

    public RCallerScriptEngine2() {
        rcaller = RCaller.create();
        rcode = RCode.create();

        rcode.addRCode("result <- list(a=0)");
        rcaller.setRCode(rcode);
        rcaller.runAndReturnResultOnline("result");
        
        context = new javax.script.SimpleScriptContext();
    }

    @Override
    public Object eval(String code, ScriptContext sc) throws ScriptException {
        return (this.eval(code));
    }

    @Override
    public Object eval(Reader reader, ScriptContext sc) throws ScriptException {
        return (this.eval(reader));
    }

    @Override
    public Object eval(String code) throws ScriptException {
        rcode.clearOnline();
        rcode.addRCode(code);
        rcode.addRCode("result <- list(a=0)");
        rcaller.setRCode(rcode);
        rcaller.runAndReturnResultOnline("result");
        return (null);
    }

    @Override
    public Object eval(Reader reader) throws ScriptException {
        BufferedReader bufferedReader = new BufferedReader(reader);
        String line;
        StringBuilder stringBuilder = new StringBuilder();

        try {
            while (true) {
                line = bufferedReader.readLine();
                if (line == null) {
                    break;
                }
                stringBuilder.append(line);
            }
        } catch (IOException ioe) {
            throw new ScriptException("Error while reading from reader: " + ioe.toString());
        }
        return (this.eval(stringBuilder.toString()));
    }

    @Override
    public Object eval(String code, Bindings bndngs) throws ScriptException {
        return (this.eval(code));
    }

    @Override
    public Object eval(Reader reader, Bindings bindings) throws ScriptException {
        return (this.eval(reader));
    }

    @Override
    public void put(String name, Object o) {
        rcode.clearOnline();
        StringBuffer code = new StringBuffer();
        RCodeUtils.addRespectToType(code, name, o, false);
        rcode.addRCode("result <- list(a=0)");
        rcode.setCode(code);

        rcaller.setRCode(rcode);
        rcaller.runAndReturnResultOnline("result");
    }

    @Override
    public Object get(String var) {
        int[] dimension;
        rcode.clearOnline();
        rcode.addRCode("result <- ls()");
        rcaller.runAndReturnResultOnline(var);
        parser = rcaller.getParser();
        parser.parse();
        try {
            //System.out.println(parser.getXMLFileAsString());
            dimension = parser.getDimensions(var);
        } catch (Exception e) {
            return (parser.getAsStringArray(var));
        }
        String vartype = parser.getType(var);
        if (dimension[0] > 1 && dimension[1] > 1) {
            return (parser.getAsDoubleMatrix(var));
        } else if (vartype.equals("numeric")) {
            return (parser.getAsDoubleArray(var));
        } else if (vartype.equals("character")) {
            return (parser.getAsStringArray(var));
        } else {
            return (parser.getAsStringArray(var)); // :o
        }
    }

    @Override
    public Bindings getBindings(int i) {
        return (this.bindings);
    }

    @Override
    public void setBindings(Bindings bndngs, int i) {
        this.bindings = bndngs;
    }

    @Override
    public Bindings createBindings() {
        return (new SimpleBindings());
    }

    @Override
    public ScriptContext getContext() {
        return (this.context);
    }

    @Override
    public void setContext(ScriptContext sc) {
        this.context = sc;
    }

    @Override
    public ScriptEngineFactory getFactory() {
        return (new RCallerScriptEngineFactory());
    }

    @Override
    public void messageReceived(String senderName, String msg) {
        System.out.println("RCaller Script Engine Received: (" + senderName + ") " + msg);
    }

    public void close() {
        this.rcaller.StopRCallerOnline();
    }

    /*
      Methods for invokable interface
     */
    @Override
    public Object invokeMethod(Object o, String fname, Object... arguments) throws ScriptException, NoSuchMethodException {
        return (invokeFunction(fname, arguments));
    }

    @Override
    public Object invokeFunction(String fname, Object... arguments) throws ScriptException, NoSuchMethodException {
        int[] dimension = null;
        String var = "fresult";

        rcode.clearOnline();
        rcode.addRCode(var + " <- " + fname + "(");
        for (int i = 0; i < arguments.length; i++) {
            NamedArgument named = (NamedArgument) arguments[i];
            RCodeUtils.addRespectToType(rcode.getCode(), named.getName(), named.getObj(), true);
            if (i != (arguments.length - 1)) {
                rcode.addRCode(",");
            }
        }
        rcode.addRCode(")");
        rcaller.setRCode(rcode);
        //Please do not delete, I like to see verbose output in development stage.
        //System.out.println(rcode.getCode().toString()); 
        rcaller.runAndReturnResultOnline(var);
       
        parser = rcaller.getParser();
        
        ArrayList<NamedArgument> namedResults = new ArrayList<>();
        ArrayList<String> names = rcaller.getParser().getNames();
        
        for (String name : names) {
            var = name;
            try {
                dimension = parser.getDimensions(var);
            } catch (Exception e) {
                namedResults.add(NamedArgument.Named(var, parser.getAsStringArray(var)));
            }
            String vartype = parser.getType(var);
            assert dimension != null;
            if (dimension[0] > 1 && dimension[1] > 1) {
                namedResults.add(NamedArgument.Named(var, parser.getAsDoubleMatrix(var)));
            } else if (vartype.equals("numeric")) {
                namedResults.add(NamedArgument.Named(var, parser.getAsDoubleArray(var)));
            } else if (vartype.equals("character")) {
                namedResults.add(NamedArgument.Named(var, parser.getAsStringArray(var)));
            } else {
                namedResults.add(NamedArgument.Named(var, parser.getAsStringArray(var)));
            }
        }
        return (namedResults);
    }

    @Override
    public <T> T getInterface(Class<T> type) {
        return (null);
    }

    @Override
    public <T> T getInterface(Object o, Class<T> type) {
        return (null);
    }

    /*
      End of Methods for invokable interface
     */
}
