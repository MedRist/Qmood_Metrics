package parsers.mood;

import com.github.javaparser.ParseResult;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.ast.AccessSpecifier;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.resolution.declarations.ResolvedReferenceTypeDeclaration;
import com.github.javaparser.symbolsolver.JavaSymbolSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.JavaParserTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver;
import com.github.javaparser.utils.SourceRoot;
import javafx.util.Pair;
import parsers.Metrics;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;


public class QMoodMetrics {
    private File file;
    private List<CompilationUnit> cus;


    public QMoodMetrics(String path) throws IOException {
        File dir = new File(
                path);
        CombinedTypeSolver typeSolver = new CombinedTypeSolver(
                new ReflectionTypeSolver(),
                new JavaParserTypeSolver(dir));
        ParserConfiguration parserConfiguration =
                new ParserConfiguration()
                        .setSymbolResolver(new JavaSymbolSolver(typeSolver));

        SourceRoot sourceRoot = new
                SourceRoot(dir.toPath());
        sourceRoot.setParserConfiguration(parserConfiguration);
        List<ParseResult<CompilationUnit>> parseResults =
                sourceRoot.tryToParse("");


        // For computing the metrics, we need to have an access to all the classes.
        // @variable allCus = All computation Units of the packages, and foreach class we create an AST.
        this.cus = parseResults.stream()
                .filter(ParseResult::isSuccessful)
                .map(r -> r.getResult().get())
                .collect(Collectors.toList());
    }

    public List<CompilationUnit> getCus() {
        return cus;
    }

    /*****

     Name of classes.

     */
    public  static  Function<List<CompilationUnit>,List<String>> nameOfClasses = (compilationUnits) -> {
        List<String> nameOfClasses = compilationUnits.
                stream().
                map(p->p.findAll(ClassOrInterfaceDeclaration.class).get(0).getNameAsString())
                .collect(Collectors.toList());
        return nameOfClasses;
    };

    /*****Design Size in Classes******/
    public  static  Function<List<CompilationUnit>,Integer> DSC = (compilationUnits) -> {
            return  compilationUnits.size();

    };


    static final BiFunction<List<CompilationUnit>,String,CompilationUnit> findCompilationUnit = (compilationUnits, ss) -> {
        CompilationUnit cu =compilationUnits.stream()
                .filter(p->p.findAll(ClassOrInterfaceDeclaration.class)
                        .get(0).getNameAsString().equals(ss))
                .findAny()
                .orElse(null);
        return cu;
    };

    /******
     *
     * Class Interface Size(CIS)
     * Messaging | Class Interface Size (CIS)
     *A count of the number of public methods in a class.
     *Interpreted as the average across all classes in a design.
     * *************/
    public  static double CIS (List<CompilationUnit>compilationUnits){
        List<String> nameClasses =nameOfClasses.apply(compilationUnits);
       int public_methods= nameClasses.stream()
                               .mapToInt(p->CIS_bis.apply(compilationUnits,p))
                               .reduce((x,y)->x+y)
                               .getAsInt();
        return (double)public_methods/nameClasses.size();
    };

    public  static   BiFunction<List<CompilationUnit>,String,Integer> CIS_bis = (compilationUnits,s) -> {
       CompilationUnit c = findCompilationUnit.apply(compilationUnits,s);
       return c.findAll(ClassOrInterfaceDeclaration.class).get(0)
                .getMethods().stream()
                .filter(p->p.getModifiers().iterator().next().asString().equalsIgnoreCase("public"))
                .collect(Collectors.toList())
                .size();
    };

    /*********
     *Number of Methods(NOM)
     * Complexity | Number of Methods (NOM)
     *A count of all the methods defined in a class.
     *Interpreted as the average across all classes in a design.
     *  ******/
    public  static double NOM (List<CompilationUnit>compilationUnits){
        List<String> nameClasses =nameOfClasses.apply(compilationUnits);
        int public_methods= nameClasses.stream()
                .mapToInt(p->NOM_bis.apply(compilationUnits,p))
                .reduce((x,y)->x+y)
                .getAsInt();
        return (double)public_methods/nameClasses.size();
    };
    public  static  BiFunction<List<CompilationUnit>,String,Integer> NOM_bis = (compilationUnits,s) -> {
        CompilationUnit c = findCompilationUnit.apply(compilationUnits,s);
        return c.findAll(ClassOrInterfaceDeclaration.class).get(0)
                .getMethods().size();
    };
     /*********
     *Number of Polymorphism methods
     *  ******/

    public double NOP()
    {
        List<String> nameClasses =nameOfClasses.apply(cus);
        int nops= nameClasses.stream()
                .mapToInt(p->NOP_bis.apply(cus,p))
                .reduce((x,y)->x+y)
                .getAsInt();
        return (double)nops/nameClasses.size();
    }
    public  static  BiFunction<List<CompilationUnit>,String,Integer> NOP_bis = (compilationUnits,s) -> {
        CompilationUnit c = findCompilationUnit.apply(compilationUnits,s);
        return c.findAll(ClassOrInterfaceDeclaration.class).get(0)
                .getMethods()
                .stream()
                .filter(p->p.asMethodDeclaration().isAnnotationPresent("Override"))
                .collect(Collectors.toList())
                .size();
    };

    /*********
     *Data Access Metrics
     * The ratio of the number of private (protected) attributes to the total number
     * of attributes declared in the class. Interpreted as the average across all design classes
     * with at least one attribute, of the ratio of non-public to total attributes in a class.
     *  ******/

    public double DAM()
    {
        List<String> nameClasses =nameOfClasses.apply(cus);
        double dam_of_classes= nameClasses.stream()
                .mapToDouble(p->DAM_bis.apply(cus,p))
                .reduce((x,y)->x+y)
                .getAsDouble();
        return (double)dam_of_classes/nameClasses.size();
    }
    public  static  BiFunction<List<CompilationUnit>,String,Double> DAM_bis = (compilationUnits,s) -> {
        CompilationUnit c = findCompilationUnit.apply(compilationUnits,s);
        ClassOrInterfaceDeclaration classe = c.findAll(ClassOrInterfaceDeclaration.class).get(0);
        ResolvedReferenceTypeDeclaration resolvedClass = classe.resolve();
        int privateOrProtected = resolvedClass.getAllFields().stream()
                .filter(f -> f.accessSpecifier() == AccessSpecifier.PRIVATE | f.accessSpecifier() == AccessSpecifier.PROTECTED)
                .collect(Collectors.toList()).size();
        return (double) privateOrProtected/resolvedClass.getAllFields().size();

    };
    /***********
     *
     * Number of
     Hierarchies
     (NOH) we will use the Depth of
     Inheritance
     Tree (DIT)

     * *********/
    public  static  Function<List<CompilationUnit>,Integer> NOH = (compilationUnits) -> {
       List<String> nameOfClasses = compilationUnits.stream()
                                                    .map(p->p.findAll(ClassOrInterfaceDeclaration.class).get(0).getNameAsString())
                                                    .collect(Collectors.toList());
       //Get the DIT of each classe
       // The Idea is to compute the dit of each class
       // If the DIT >1 so we consider one Hierarchy
      int numberOfHierarchies= nameOfClasses.stream()
                    // we test for each class the DIT value, if DIT > 1 then we count it as one Hierarchy
                    .filter(p->Metrics.DIT.apply(compilationUnits,p)>1)
                    .collect(Collectors.toList())
                    .size();
        return numberOfHierarchies;


    };


    /***********
     * Composition | Measure of Aggregation (MOA)
     * A count of the number of data declarations whose types are user-defined classes.
     * Interpreted as the average value across all design classes. We define ‘user defined classes’ as
     * non-primitive types that are not included in the Java
     * standard libraries and collections of user-defined classes from the java.util.collections package.
     * *******/

    public  static  Function<List<CompilationUnit>,Integer> MOA = (compilationUnits) -> {
        List<ClassOrInterfaceDeclaration> classes = compilationUnits.stream()
                .map(p->p.findAll(ClassOrInterfaceDeclaration.class).get(0))
                .collect(Collectors.toList());
        List<String> nameOfClasses = compilationUnits.stream()
                                    .map(p->p.findAll(ClassOrInterfaceDeclaration.class).get(0).getNameAsString())
                                    .collect(Collectors.toList());

        List<Pair<String,List<String>>>  liste = new LinkedList<>();
        FieldsTypeVisitor visitor = new FieldsTypeVisitor();
        List<List<String>> typesOfEachClasse = classes.stream()
                .map(p->{
                    List<String> listeOfTypes = new LinkedList<>();
                    visitor.visit(p,listeOfTypes);
                    return listeOfTypes;
                })
                .collect(Collectors.toList());
         ///////////////////////////////////////////
        int count =0;
        for (List<String> l : typesOfEachClasse)
            for (String s : l)
                if (nameOfClasses.contains(s)) count++;

        return count;
    };


    /***********
     * Abstraction | Average Number of Ancestors (ANA)
     *The average number of
     *classes from which each class inherits information.
     * *******/

    public  static  Function<List<CompilationUnit>,Double> ANA = (compilationUnits) -> {
        List<String> nameOfClasses = compilationUnits.
                stream().
                map(p->p.findAll(ClassOrInterfaceDeclaration.class).get(0).getNameAsString())
                .collect(Collectors.toList());
       int result =   nameOfClasses.stream()
                     .mapToInt(p->Metrics.NOC.apply(compilationUnits,p))
                     .filter(p->p>=1)
                     .reduce((x,y)->x+y)
                     .getAsInt();
       return (double)result/nameOfClasses.size();

    };


    public double understandability()
    {
        return -0.33* ANA.apply(cus)+
                0.33*DAM()+
                0.33*NOP()
                +0.33*NOM(cus)
                +0.33*DSC.apply(cus);

        //-0.33*Abstraction+0.33*Encapsulation0.33* Coupling+0.33*Cohesion0.33*Plomorphism-0.33*Complexity0.33*Design Size
    }
}
