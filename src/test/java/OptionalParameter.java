public class OptionalParameter<EmbeddedParameterType extends UserParameter<?, ?>>
        implements UserParameter<Boolean, OptionalParameterComponent<?>>

{
    private EmbeddedParameterType embeddedParameter;
}
enum EqualitySign {GREATER,GREATER_EQUAL, EQUAL, ....;
    public String toString() { return switch(this) { case EQUAL -> "="; case .... }
    }
    record ConditionalInteger(int value, EqualitySign comparator) {
        public boolean acceptValue(int testValue) {
            return switch(comparator) {
                case EQUAL -> value == testValue;
                case ....
            }
        }
    }