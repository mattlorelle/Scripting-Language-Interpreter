package plc.homework;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * Contains JUnit tests for {@link Regex}. Test structure for steps 1 & 2 are
 * provided, you must create this yourself for step 3.
 *
 * To run tests, either click the run icon on the left margin, which can be used
 * to run all tests or only a specific test. You should make sure your tests are
 * run through IntelliJ (File > Settings > Build, Execution, Deployment > Build
 * Tools > Gradle > Run tests using <em>IntelliJ IDEA</em>). This ensures the
 * name and inputs for the tests are displayed correctly in the run window.
 */
public class RegexTests {

    /**
     * This is a parameterized test for the {@link Regex#EMAIL} regex. The
     * {@link ParameterizedTest} annotation defines this method as a
     * parameterized test, and {@link MethodSource} tells JUnit to look for the
     * static method {@link #testEmailRegex()}.
     *
     * For personal preference, I include a test name as the first parameter
     * which describes what that test should be testing - this is visible in
     * IntelliJ when running the tests (see above note if not working).
     */
    @ParameterizedTest
    @MethodSource
    public void testEmailRegex(String test, String input, boolean success) {
        test(input, Regex.EMAIL, success);
    }

    /**
     * This is the factory method providing test cases for the parameterized
     * test above - note that it is static, takes no arguments, and has the same
     * name as the test. The {@link Arguments} object contains the arguments for
     * each test to be passed to the function above.
     */
    public static Stream<Arguments> testEmailRegex() {
        return Stream.of(
                Arguments.of("Alphanumeric", "thelegend27@gmail.com", true),
                Arguments.of("UF Domain", "otherdomain@ufl.edu", true),
                Arguments.of("Underscore","firstname_lastname@gmail.com",true),
                Arguments.of("All caps","DRDOBBINS@aol.com",true),
                Arguments.of("All same letter","aaaaaa@aaa.aa",true),

                Arguments.of("Missing Domain Dot", "missingdot@gmailcom", false),
                Arguments.of("Symbols", "symbols#$%@gmail.com", false),
                Arguments.of("Underscore after @","firstnamelastname@_gmail.com",false),
                Arguments.of("Illegal suffix","DrDobbins@aol.a",false),
                Arguments.of("Missing Email Domain","missingdomain@",false)
        );
    }

    @ParameterizedTest
    @MethodSource
    public void testEvenStringsRegex(String test, String input, boolean success) {
        test(input, Regex.EVEN_STRINGS, success);
    }

    public static Stream<Arguments> testEvenStringsRegex() {
        return Stream.of(
                //what has ten letters and starts with gas?
                Arguments.of("10 Characters", "automobile", true),
                Arguments.of("14 Characters", "i<3pancakes10!", true),
                Arguments.of("12 Characters","abcdefghijkl",true),
                Arguments.of("16 Characters","Under_score_test",true),
                Arguments.of("20 Characters","1,2,3,4,5,6,7,8,9,10",true),

                Arguments.of("6 Characters", "6chars", false),
                Arguments.of("13 Characters", "i<3pancakes9!", false),
                Arguments.of("1 Character","F",false),
                Arguments.of("20+ characters","This string has more than twenty characters",false),
                Arguments.of("19 Characters","0zS7Qv9q0s8xxnPS1vb",false)
        );
    }

    @ParameterizedTest
    @MethodSource
    public void testIntegerListRegex(String test, String input, boolean success) {
        test(input, Regex.INTEGER_LIST, success);
    }

    public static Stream<Arguments> testIntegerListRegex() {
        return Stream.of(
                Arguments.of("Single Element", "[1]", true),
                Arguments.of("Multiple Elements", "[1,2,3]", true),
                Arguments.of("Empty list","[]",true),
                Arguments.of("Space before comma","[1 ,2 ,3 ,4]",true),
                Arguments.of("Inconsistent comma","[1, 2 , 3 ,4]",true),

                Arguments.of("Missing Brackets", "1,2,3", false),
                Arguments.of("Missing Commas", "[1 2 3]", false),
                Arguments.of("Trailing comma","[1,2,3,4,]",false),
                Arguments.of("Missing brackets, trailing comma","1,2,3,4,5,",false),
                Arguments.of("Empty list with space","[ ]",false)
        );
    }

    @ParameterizedTest
    @MethodSource
    public void testNumberRegex(String test, String input, boolean success) {
        test(input, Regex.NUMBER, success); //TODO
    }

    public static Stream<Arguments> testNumberRegex() {

        return Stream.of(
                Arguments.of("Positive int","5",true),
                Arguments.of("Negative int","-5",true),
                Arguments.of("Positive decimal","10.75",true),
                Arguments.of("Negative decimal","-15.33",true),
                Arguments.of("Leading and trailing zero decimal","05.1450",true),

                Arguments.of("Trailing decimal","2.",false),
                Arguments.of("Leading decimal",".24",false),
                Arguments.of("Double decimal","12..67",false),
                Arguments.of("Space after decimal","12. 67",false),
                Arguments.of("Space before decimal","12 .67",false)
        );
    }

    @ParameterizedTest
    @MethodSource
    public void testStringRegex(String test, String input, boolean success) {
        test(input, Regex.STRING, success); //TODO
    }

    public static Stream<Arguments> testStringRegex() {

        return Stream.of(
                Arguments.of("Empty quotes","\"\"",true),
                Arguments.of("Empty quotes with space","\" \"",true),
                Arguments.of("Hello world","\"Hello, World!\"",true),
                Arguments.of("Correct escape","\"1\t2\"",true),
                Arguments.of("symbols","\"!@#$%^&*()\"",true),

                Arguments.of("Empty string","",false),
                Arguments.of("Unterminated","\"unterminated",false),
                Arguments.of("Invalid escape","\"invalid\\escape\"",false),
                Arguments.of("Middle quotation","\"Hello, \"World!\"",false),
                Arguments.of("Missing first quote","Hello, World!\"",false)
        );
    }

    /**
     * Asserts that the input matches the given pattern. This method doesn't do
     * much now, but you will see this concept in future assignments.
     */
    private static void test(String input, Pattern pattern, boolean success) {
        Assertions.assertEquals(success, pattern.matcher(input).matches());
    }

}
