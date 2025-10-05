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
