package lima.tools;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Enumeration;
import java.io.PrintStream;

public class ProgramBase {
	protected int verbose = 1;
	protected boolean log_is_std = false;
	protected boolean err_is_std = false;
	protected boolean out_is_std = false;
	protected boolean closed = false;
	protected Hashtable<String, String> parameters = null;
	public PrintStream errorfile = null;
	public PrintStream outfile = null;
	public PrintStream logfile = null;

	// params : liste des paramètres à récupérer.
	// useStdParams : indique s'il faut chercher les paramètres standard (logfile, errorfile, outfile, verbose).
	public ProgramBase(String[] args, ArrayList<String> params, boolean useStdParams) throws Exception {
		init(args, params, useStdParams);
	};
	// Cherche les paramètres demandés (ainsi que les paramètres standard).
	public ProgramBase(String[] args, ArrayList<String> params) throws Exception {
		this(args, params, true);
	};
	// Récupère tous les arguments disponibles.
	public ProgramBase(String[] args) throws Exception {
		this(args, null, false);
	};
	public ProgramBase(String[] args, String ... parameters) throws Exception {
		ArrayList<String> params = new ArrayList<String>(parameters.length);
		for(String parameter : parameters) params.add(parameter);
		init(args, params, true);
	}
	protected void init(String[] args, ArrayList<String> params, boolean useStdParams) throws Exception {
		if(params == null) params = new ArrayList<String>();
		if(useStdParams) {
			if(!params.contains("logfile")) params.add("logfile");
			if(!params.contains("errorfile")) params.add("errorfile");
			if(!params.contains("outfile")) params.add("outfile");
			if(!params.contains("verbose")) params.add("verbose");
		};
		this.parameters = new Hashtable<String, String>();
		this.parseParameters(args, params);
	};

	protected void parseParameters(String[] args, ArrayList<String> paramlist) throws Exception {
		if(args.length > 0) {
			/*Recuperation des parametres.*/
			int compteParametres = paramlist.size();
			for(int i = 0; i < args.length; ++i) {
				String argument = args[i];
				if(compteParametres > 0) {
					int j = -1;
					for(j = 0; j < compteParametres && argument.indexOf(paramlist.get(j) + "=") != 0; ++j);
					if(j < compteParametres) {
						String parametre = paramlist.get(j);
						if(!this.parameters.containsKey(parametre)) {
							this.parameters.put(parametre, argument.substring(parametre.length() + 1));
						};
					};
				} else if(argument.indexOf("=") > 0) {
					String[] morceaux = argument.split("=", 2);
					if(morceaux.length == 2 && !this.parameters.containsKey(morceaux[0])) {
						this.parameters.put(morceaux[0], morceaux[1]);
					};
				};
			};
			/*Test des parametres standards.*/
			if(this.hasParameter("logfile")) {
				this.logfile = new PrintStream(this.parameter("logfile"));
			};
			if(this.hasParameter("outfile")) {
				this.outfile = new PrintStream(this.parameter("outfile"));
			};
			if(this.hasParameter("errorfile")) {
				this.errorfile = new PrintStream(this.parameter("errorfile"));
			};
			if(this.hasParameter("verbose")) {
				String valeur = this.parameter("verbose");
				if(valeur.equals("0") || valeur.equals("min")) this.verbose = 0;
				else if(valeur.equals("1") || valeur.equals("normal")) this.verbose = 1;
				else if(valeur.equals("2") || valeur.equals("max")) this.verbose = 2;
			};
		};
		/*Valeurs par defaut pour les parametres standard.*/
		if(this.logfile == null) {
			this.logfile = System.err;
			this.log_is_std = true;
		};
		if(this.errorfile == null) {
			this.errorfile = System.err;
			this.err_is_std = true;
		};
		if(this.outfile == null) {
			this.outfile = System.out;
			this.out_is_std = true;
		};
	}

	public boolean hasParameter() {
		return !this.parameters.isEmpty();
	};
	public boolean hasParameter(String key) {
		return this.parameters.containsKey(key);
	};

	public String parameter(String key) {
		/*Retourne un String ou null.*/
		return this.parameters.get(key);
	}

	public void fatalError(CharSequence msg) throws Exception {
		this.error(msg);
		throw new Exception("Fatal error.");
	}

	public void error(CharSequence msg) throws Exception {
		this.error(msg, this.verbose);
	};
	public void error(CharSequence msg, int verbose) throws Exception {
		if(verbose <= this.verbose) this.writeToBuffer(this.errorfile, msg);
	};

	public void log(CharSequence msg) throws Exception {
		this.log(msg, this.verbose);
	};
	public void log(CharSequence msg, int verbose) throws Exception {
		if(verbose <= this.verbose) this.writeToBuffer(this.logfile, msg);
	};

	public void out(CharSequence msg) throws Exception {
		this.writeToBuffer(this.outfile, msg);
	};

	protected void writeToBuffer(PrintStream b, CharSequence msg) throws Exception {
		b.print(msg);
	};

	public void end() throws Exception {
		if(!this.closed) {
			if(this.logfile != null && !this.log_is_std) this.logfile.close();
			if(this.errorfile != null && !this.err_is_std) this.errorfile.close();
			if(this.outfile != null && !this.out_is_std) this.outfile.close();
			this.closed = true;
		}
	};

	/*Polymorphisme : object.toString()*/
	public String toString() {
		StringBuffer s = new StringBuffer();
		Enumeration<String> cles = this.parameters.keys();
		while(cles.hasMoreElements()) {
			String cle = cles.nextElement();
			s.append(cle).append(" : ").append(this.parameters.get(cle)).append(System.getProperty("line.separator"));
		};
		s.append("<verbose> : ").append(this.verbose).append(System.getProperty("line.separator"));
		return s.toString();
	};
	/*Polymorphisme : object.finalize()*/
	protected void finalize() throws Throwable {
		this.end();
	};

	static public void main(String[] args) {
		try {
			ProgramBase base = new ProgramBase(args);
			base.error("error message\n");
			base.log("log message\n");
			base.out("out message\n");
			System.out.println(base);
			base.end();
		} catch(Exception e) {
			e.printStackTrace();
		};
	};
};

