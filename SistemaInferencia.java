import java.util.*;
import java.util.regex.*;
import java.io.*;


// Clase para representar un término (constante o variable)
class Term {
    public String name;
    public boolean isVariable;
    
    public Term(String name, boolean isVariable) {
        this.name = name;
        this.isVariable = isVariable;
    }
    
    public Term copy() {
        return new Term(this.name, this.isVariable);
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Term term = (Term) obj;
        return isVariable == term.isVariable && name.equals(term.name);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(name, isVariable);
    }
    
    @Override
    public String toString() {
        return name;
    }
}

// Clase para representar un predicado
class Predicate {
    public String name;
    public List<Term> terms;
    public boolean negated;
    
    public Predicate(String name, List<Term> terms, boolean negated) {
        this.name = name;
        this.terms = terms;
        this.negated = negated;
    }
    
    public Predicate copy() {
        List<Term> newTerms = new ArrayList<>();
        for (Term term : terms) {
            newTerms.add(term.copy());
        }
        return new Predicate(this.name, newTerms, this.negated);
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Predicate pred = (Predicate) obj;
        return negated == pred.negated && name.equals(pred.name) && terms.equals(pred.terms);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(name, terms, negated);
    }
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        if (negated) sb.append("¬");
        sb.append(name);
        if (!terms.isEmpty()) {
            sb.append("(");
            for (int i = 0; i < terms.size(); i++) {
                sb.append(terms.get(i));
                if (i < terms.size() - 1) sb.append(", ");
            }
            sb.append(")");
        }
        return sb.toString();
    }
}

// Clase para representar una cláusula
class Clause {
    public List<Predicate> predicates;
    public int id;
    
    public Clause(List<Predicate> predicates, int id) {
        this.predicates = predicates;
        this.id = id;
    }
    
    public Clause copy() {
        List<Predicate> newPredicates = new ArrayList<>();
        for (Predicate pred : predicates) {
            newPredicates.add(pred.copy());
        }
        return new Clause(newPredicates, this.id);
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Clause clause = (Clause) obj;
        return predicates.equals(clause.predicates);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(predicates);
    }
    
    @Override
    public String toString() {
        if (predicates.isEmpty()) return "□";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < predicates.size(); i++) {
            sb.append(predicates.get(i));
            if (i < predicates.size() - 1) sb.append(" ∨ ");
        }
        return sb.toString();
    }
}

//-----Sistema de Unificación-----

// Clase para manejar la unificación
class Unifier {
    
    // Aplica una sustitución a un término
    public static Term applySubstitution(Term term, Map<String, Term> substitution) {
        if (term.isVariable && substitution.containsKey(term.name)) {
            return substitution.get(term.name).copy();
        }
        return term.copy();
    }
    
    // Aplica una sustitución a un predicado
    public static Predicate applySubstitution(Predicate predicate, Map<String, Term> substitution) {
        List<Term> newTerms = new ArrayList<>();
        for (Term term : predicate.terms) {
            newTerms.add(applySubstitution(term, substitution));
        }
        return new Predicate(predicate.name, newTerms, predicate.negated);
    }
    
    // Aplica una sustitución a una cláusula
    public static Clause applySubstitution(Clause clause, Map<String, Term> substitution) {
        List<Predicate> newPredicates = new ArrayList<>();
        for (Predicate pred : clause.predicates) {
            newPredicates.add(applySubstitution(pred, substitution));
        }
        return new Clause(newPredicates, clause.id);
    }
    
    public static Map<String, Term> unify(Predicate p1, Predicate p2) {
        if (!p1.name.equals(p2.name) || p1.terms.size() != p2.terms.size() || p1.negated == p2.negated) {
            return null;
        }
        
        Map<String, Term> substitution = new HashMap<>();
        
        for (int i = 0; i < p1.terms.size(); i++) {
            Term t1 = p1.terms.get(i);
            Term t2 = p2.terms.get(i);
            
            Map<String, Term> newSubs = unifyTerms(t1, t2, substitution);
            if (newSubs == null) {
                return null;
            }
            substitution.putAll(newSubs);
        }
        
        return substitution;
    }
    
    private static Map<String, Term> unifyTerms(Term t1, Term t2, Map<String, Term> currentSub) {
        Map<String, Term> substitution = new HashMap<>(currentSub);
        
        // Aplicar sustitución actual
        t1 = applySubstitution(t1, substitution);
        t2 = applySubstitution(t2, substitution);
        
        if (t1.equals(t2)) {
            return substitution;
        }
        
        if (t1.isVariable) {
            if (occursCheck(t1.name, t2, substitution)) {
                return null;
            }
            substitution.put(t1.name, t2);
            return substitution;
        }
        
        if (t2.isVariable) {
            if (occursCheck(t2.name, t1, substitution)) {
                return null;
            }
            substitution.put(t2.name, t1);
            return substitution;
        }
        
        return null; // No se pueden unificar
    }
    
    private static boolean occursCheck(String varName, Term term, Map<String, Term> substitution) {
        term = applySubstitution(term, substitution);
        
        if (term.isVariable) {
            return term.name.equals(varName);
        }
        
        return false;
    }
}

//-----Convertidor a Forma Normal Conjuntiva (FNC)-----

// Clase para convertir a Forma Normal Conjuntiva
class FNCConverter {
    
    public static List<Clause> convertToFNC(List<String> sentences) {
        List<Clause> clauses = new ArrayList<>();
        int clauseId = 1;
        
        for (String sentence : sentences) {
            List<Clause> sentenceClauses = processSentence(sentence, clauseId);
            clauses.addAll(sentenceClauses);
            clauseId += sentenceClauses.size();
        }
        
        return standardizeVariables(clauses);
    }
    
    private static List<Clause> standardizeVariables(List<Clause> clauses) {
        List<Clause> standardized = new ArrayList<>();
        int varCounter = 1;
        
        for (Clause clause : clauses) {
            Map<String, String> varMap = new HashMap<>();
            List<Predicate> newPredicates = new ArrayList<>();
            
            for (Predicate pred : clause.predicates) {
                List<Term> newTerms = new ArrayList<>();
                for (Term term : pred.terms) {
                    if (term.isVariable) {
                        if (!varMap.containsKey(term.name)) {
                            varMap.put(term.name, "v" + varCounter++);
                        }
                        newTerms.add(new Term(varMap.get(term.name), true));
                    } else {
                        newTerms.add(term.copy());
                    }
                }
                newPredicates.add(new Predicate(pred.name, newTerms, pred.negated));
            }
            standardized.add(new Clause(newPredicates, clause.id));
        }
        
        return standardized;
    }
    
    private static List<Clause> processSentence(String sentence, int startId) {
        List<Clause> clauses = new ArrayList<>();
        sentence = sentence.trim();     // Eliminar espacios en blanco extra
        
        // Procesar diferentes tipos de sentencias
        if (sentence.contains("⇒")) {
            // Implicación: A ⇒ B se convierte a ¬A ∨ B
            String[] parts = sentence.split("⇒", 2);
            String left = parts[0].trim();
            String right = parts[1].trim();
            
            List<Predicate> leftPreds = extractPredicates(left, true);
            List<Predicate> rightPreds = extractPredicates(right, false);
            
            List<Predicate> allPreds = new ArrayList<>();
            allPreds.addAll(leftPreds);
            allPreds.addAll(rightPreds);
            
            clauses.add(new Clause(allPreds, startId));
            
        } else if (sentence.contains("∀")) {
            // Cuantificador universal - eliminar y procesar
            sentence = removeQuantifiers(sentence);
            clauses.addAll(processSentence(sentence, startId));
            
        } else if (sentence.contains("∧")) {
            // Conjunción: separar en múltiples cláusulas
            String[] conjunctions = sentence.split("∧");
            for (String conj : conjunctions) {
                List<Predicate> preds = extractPredicates(conj.trim(), false);
                clauses.add(new Clause(preds, startId++));
            }
        } else {
             // Predicado simple
            List<Predicate> preds = extractPredicates(sentence, false);
            clauses.add(new Clause(preds, startId));
        }
        
        return clauses;
    }
    
    private static String removeQuantifiers(String sentence) {
        return sentence.replaceAll("∀[a-zA-Z]", "").replaceAll("∃[a-zA-Z]", "").trim();
    }
    
    private static List<Predicate> extractPredicates(String text, boolean negate) {
        List<Predicate> predicates = new ArrayList<>();
        
        // Manejar negación
        if (text.startsWith("¬")) {
            negate = !negate;
            text = text.substring(1).trim();
        }
        
        // Verificar si es un predicado con parámetros
        if (text.contains("(") && text.contains(")")) {
            Pattern pattern = Pattern.compile("([a-zA-Z]+)\\(([^)]+)\\)");
            Matcher matcher = pattern.matcher(text);
            
            while (matcher.find()) {
                String predName = matcher.group(1);
                String params = matcher.group(2);
                List<Term> terms = parseTerms(params);
                predicates.add(new Predicate(predName, terms, negate));
            }
        } else {
            // Predicado sin parámetros
            predicates.add(new Predicate(text, new ArrayList<>(), negate));
        }
        
        return predicates;
    }
    
    private static List<Term> parseTerms(String params) {
        List<Term> terms = new ArrayList<>();
        String[] termStrs = params.split(",");
        
        for (String termStr : termStrs) {
            termStr = termStr.trim();
            // Determinar si es variable minúscula o constante mayúscula
            boolean isVariable = Character.isLowerCase(termStr.charAt(0));
            terms.add(new Term(termStr, isVariable));
        }
        
        return terms;
    }
    
    public static List<Predicate> parsePredicates(String text, boolean negate) {
        return extractPredicates(text, negate);
    }
}

//-----Algoritmo de Resolución-----

// Clase principal del motor de inferencia
class ResolutionEngine {
    private List<Clause> clauses;
    private PrintWriter logWriter;
    private int nextClauseId;
    
    public ResolutionEngine() {
        this.clauses = new ArrayList<>();
        this.nextClauseId = 1;
    }
    
    public void initialize(List<String> sentences, String logFilePath) throws IOException {
        this.logWriter = new PrintWriter(new FileWriter(logFilePath));
        
        // Convertir a FNC
        List<Clause> initialClauses = FNCConverter.convertToFNC(sentences);
        this.clauses.addAll(initialClauses);
        this.nextClauseId = initialClauses.size() + 1;
        
        // Guardar cláusulas iniciales
        logWriter.println("=== CLAUSULAS EN FORMA NORMAL CONJUNTIVA ===");
        for (Clause clause : clauses) {
            logWriter.println("C" + clause.id + ": " + clause);
        }
        logWriter.println();
        logWriter.flush();
    }
    
    public boolean resolve(String query) {
        logWriter.println("=== ALGORITMO DE RESOLUCIÓN ===");
        logWriter.println("Consulta: " + query);
        logWriter.println();
        
        // Negar la consulta y agregar a las cláusulas
        List<Clause> tempClauses = new ArrayList<>(clauses);
        List<Predicate> queryPreds = FNCConverter.parsePredicates(query, true);
        Clause negatedQuery = new Clause(queryPreds, nextClauseId++);
        tempClauses.add(negatedQuery);
        
        logWriter.println("Agregando negación de consulta: C" + negatedQuery.id + ": " + negatedQuery);
        logWriter.println();
        
        int step = 1;
        Set<String> usedPairs = new HashSet<>();
        
        while (true) {
            boolean newClauseGenerated = false;
            
            for (int i = 0; i < tempClauses.size(); i++) {
                for (int j = i + 1; j < tempClauses.size(); j++) {
                    Clause c1 = tempClauses.get(i);
                    Clause c2 = tempClauses.get(j);
                    
                    String pairKey = Math.min(c1.id, c2.id) + "-" + Math.max(c1.id, c2.id);
                    if (usedPairs.contains(pairKey)) continue;
                    
                    Clause resolvent = resolveClauses(c1, c2);
                    if (resolvent != null) {
                        usedPairs.add(pairKey);
                        
                        logWriter.println("Paso " + step++ + ":");
                        logWriter.println("Resolviendo C" + c1.id + " y C" + c2.id);
                        logWriter.println("C" + c1.id + ": " + c1);
                        logWriter.println("C" + c2.id + ": " + c2);
                        logWriter.println("Resolvente: " + resolvent);
                        logWriter.println();
                        logWriter.flush();
                        
                        if (resolvent.predicates.isEmpty()) {
                            logWriter.println("¡Se encontró la cláusula vacía! La consulta es verdadera.");
                            logWriter.close();
                            return true;
                        }
                        
                        // Verificar si la cláusula ya existe
                        boolean exists = false;
                        for (Clause existing : tempClauses) {
                            if (existing.equals(resolvent)) {
                                exists = true;
                                break;
                            }
                        }
                        
                        if (!exists) {
                            resolvent.id = nextClauseId++;
                            tempClauses.add(resolvent);
                            newClauseGenerated = true;
                        }
                    }
                }
            }
            
            if (!newClauseGenerated) {
                logWriter.println("No se pueden generar más cláusulas. La consulta es falsa.");
                logWriter.close();
                return false;
            }
        }
    }
    
    private Clause resolveClauses(Clause c1, Clause c2) {
        for (Predicate p1 : c1.predicates) {
            for (Predicate p2 : c2.predicates) {
                if (p1.name.equals(p2.name) && p1.negated != p2.negated) {
                    Map<String, Term> substitution = Unifier.unify(p1, p2);
                    if (substitution != null) {
                        // Crear resolvente
                        List<Predicate> newPredicates = new ArrayList<>();
                        
                        // Agregar todos los predicados de c1 excepto p1
                        for (Predicate pred : c1.predicates) {
                            if (!pred.equals(p1)) {
                                newPredicates.add(Unifier.applySubstitution(pred, substitution));
                            }
                        }
                        
                        // Agregar todos los predicados de c2 excepto p2
                        for (Predicate pred : c2.predicates) {
                            if (!pred.equals(p2)) {
                                newPredicates.add(Unifier.applySubstitution(pred, substitution));
                            }
                        }
                        
                        // Eliminar duplicados
                        List<Predicate> uniquePreds = new ArrayList<>();
                        for (Predicate pred : newPredicates) {
                            if (!containsPredicate(uniquePreds, pred)) {
                                uniquePreds.add(pred);
                            }
                        }
                        
                        return new Clause(uniquePreds, -1);     // ID temporal
                    }
                }
            }
        }
        return null;
    }
    
    private boolean containsPredicate(List<Predicate> list, Predicate pred) {
        for (Predicate p : list) {
            if (p.equals(pred)) return true;
        }
        return false;
    }
}
