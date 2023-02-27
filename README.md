
# VEML (Violet's Efficient Markup Language)

## General Formatting

- Comments are marked with //, VEML will ignore any content from the comment mark until the end of the line
- VEML is not strict about whitespace,  Using multiple spaces or empty lines instead of a single one is allowed.
- VEML is case sensitive, sotwo names which are different only in CaSiNg will be considered two separate entities.

## Primitive Values

    Property = Value

Property **name** must be valid according to Java naming rules
Values can be specified as follows:

### Strings

    myString = "string value"   
    myString = "string value with  \\"quotes\\" "

### Characters

    myChar = 'c'  
    myChar = '\\n'  
    myChar = '\\u0000'

### Numeric values

    myInt = 1 *or* 1i  *or*  1I  
    myDbl = 2.0 *or* 2.0d *or* 2.0D
    myShort = 1s *or* 1S
    myFloat = 1.0f *or* 1.0F
    myLong = 1l *or* 1L
    myByte = 1b *or* 1B

### Boolean values
    myBool = true
    myBool = false

### Other
    myVar = null

### Assigning Classes
    myClassVariable = java.util.Vector.class

## Objects and Sections

For the following Java object:

    Rectangle myRect = new Rectangle(50, 100);  // height = 50, width = 100

One valid VEML sytanx is

    myRect.height = 50
    myRect.width = 100

In this case VEML will implicitly create an object named **myRect** and set the values of the height and width properties.
When there are many properties it is inconvenient to prefix every one with the object path. To make this easier, VEML allows defining a **Section** in which all properties have the same path

The above example could be written as follows:

    {myRect}
    height = 50
    width = 100

In this notation we have defined a **Section** in which all the properties are scoped to the MyRect Object.

The scope of a **Section** continues until another **Section** begins, or until it is explicitly closed by using {}./
Example:

    {myRect}
    height = 50
    width = 100

    {MyCircle}
    center.X = 10
    center.Y = 10
    radius = 100

    {}

### Typed Objects
The concrete Java type of the object can be specified in cases where it isn't clear (for example when it will be deserialized into an interface or abstract base class).  The type name must be fully qualified, for example **java.io.File**

    {myRect} **package.Rectangle**
    height = 50
    width = 100

### Nested Objects

Properties can be objects which contain properties, which can also be objects which contain properties, and so on.  So objects can be arbirarily nested to any depth.  
Any place an object name can be used, a full or partial path can be specified. 


Examples:

    object1.object2.object3.name = "Joe"

and

    {object1}
    object2.object3.name = "Joe"

and

    {object1.object2}
    object3.name = "Joe"

and

    {object1.object2.object3}
    name = "Joe"

and 

    {object1.object2.object3.name} = "Joe"

## Arrays

Arrays need to contain values of the same type (same Java superclass or interface is also allowed)
    
    myArray = [ 1, 2, 3 ]
    myArray = [ true, true, false ]
    myArray = [ "good morning", "good afternoon", "good night" ]

Similar to Object, Array types can be explicitly set by appending the java class name after the closing bracket

     myArray = [  1, 3, 5 ] int.class

Arrays ignore line breaks and can be spread across multiple lines:
    
    myArray = [ 1, 
            2 , 3
            ]

Arrays can be nested:

    myArray = [  1, "hello", [1, 2, 3] ] java.lang.Object

In the above example, the outer array is typed as Object to allow different element types, and the last element in the list is an Array of ints. 


## Arrays of Objects

Arrays can hold objects, in which case the syntax changes:

    |myArrayofRectangles| package.Rectangle
    
    [myArrayofRectangles]
    height = 50
    width = 100

    [myArrayofRectangles]
    height = 20
    width = 30

That example defines an array of **Rectangle** objects containing two elements.

Each element can also be explicitly typed to any type which meets the constraint of the array type.
Assuming the existence of a **Square** class inheriting from **Rectangle**, the following would also be valid:

    |myArrayofRectangles| package.Rectangle

    [myArrayofRectangles] package.Rectangle
    height = 50
    width = 100

    [myArrayofRectangles] package.Square
    side = 10

Indexes in an object array can be set to a primitive value as well:

    |myArrayofRectangles|

    [myArrayofRectangles] = null
    [myArrayofRectangles] = 1

## Object References

It is possible to reference values defined elsewhere in a VEML file. The following are equivalent:

    object1.name = "Joe"
    object2.name = "Joe"

and

    object1.name = "Joe"
    object2.name = object1.name


References can also include arrays and array indexes:

    myArray2 = myArray

and

    object2.name = myArray[1].someObject.name

and

    myArray[1][0].someObject.name = object2.name

Notes: 
* With references it is possible to create object graphs and circular references.
* Values cannot be changed, so in the last example the `myArray[1].someObject.name` property cannot have already been assigned a value.

It is also possible to add an index to an array using `[]` before an equals sign (`=`) if there is one:

    myArray[].someObject.name = "name"

and

    {myArray[].someObject}
    name = "name"

and

    [myArray[].objectArray]
        
After the equals sign (`=`) if there is one `[]` gets the last index of an array:

    myArray[].someObject.name = myArray[]
