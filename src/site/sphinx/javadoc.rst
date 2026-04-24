
.. raw:: html

    <div id="floating-toc">
        <div class="search-container">
            <input type="button" id="toc-hide-show-btn"></input>
            <input type="text" id="toc-search" placeholder="Search" />
        </div>
        <ul id="toc-list"></ul>
    </div>



#######################################################################
API 1.3.0-SNAPSHOT
#######################################################################

Base Package: com.github.markusbernhardt.xmldoclet


..  _com.github.markusbernhardt.xmldoclet:
***********************************************************************
Base
***********************************************************************

..  _com.github.markusbernhardt.xmldoclet.Parser:

=======================================================================
Parser
=======================================================================

*extends:* :ref:`Object<java.lang.Object>` 

| The main parser class. It scans the given Doclet document root and creates the XML tree.

| **Parser** ()


| **parseRootDoc** (rootDoc) → :ref:`Root<com.github.markusbernhardt.xmldoclet.xjc.Root>`
| The entry point into parsing the javadoc.
|          :ref:`RootDoc<com.sun.javadoc.RootDoc>` rootDoc  | rootDoc The RootDoc intstance obtained via the doclet API
|          returns :ref:`Root<com.github.markusbernhardt.xmldoclet.xjc.Root>`  | The root node, containing everything parsed from javadoc doclet




                |          :ref:`PackageDoc<com.sun.javadoc.PackageDoc>` packageDoc

                |          returns :ref:`Package<com.github.markusbernhardt.xmldoclet.xjc.Package>`


            
                Parse an annotation.
                
                
                |          :ref:`AnnotationTypeDoc<com.sun.javadoc.AnnotationTypeDoc>` annotationTypeDoc  | annotationTypeDoc A AnnotationTypeDoc instance

                |          returns :ref:`Annotation<com.github.markusbernhardt.xmldoclet.xjc.Annotation>`  | the annotation node


            
                Parse the elements of an annotation
                
                
                |          :ref:`AnnotationTypeElementDoc<com.sun.javadoc.AnnotationTypeElementDoc>` annotationTypeElementDoc  | annotationTypeElementDoc A AnnotationTypeElementDoc instance

                |          returns :ref:`AnnotationElement<com.github.markusbernhardt.xmldoclet.xjc.AnnotationElement>`  | the annotation element node


            
                Parses annotation instances of an annotable program element
                
                
                
                |          :ref:`AnnotationDesc<com.sun.javadoc.AnnotationDesc>` annotationDesc  | annotationDesc annotationDesc

                |          :ref:`String<java.lang.String>` programElement  | programElement programElement

                |          returns :ref:`AnnotationInstance<com.github.markusbernhardt.xmldoclet.xjc.AnnotationInstance>`  | representation of annotations


            
                |          :ref:`ClassDoc<com.sun.javadoc.ClassDoc>` classDoc

                |          returns :ref:`Enum<com.github.markusbernhardt.xmldoclet.xjc.Enum>`


            
                Parses an enum type definition
                
                
                |          :ref:`FieldDoc<com.sun.javadoc.FieldDoc>` fieldDoc

                |          returns :ref:`EnumConstant<com.github.markusbernhardt.xmldoclet.xjc.EnumConstant>`  | 


            
                |          :ref:`ClassDoc<com.sun.javadoc.ClassDoc>` classDoc

                |          returns :ref:`Interface<com.github.markusbernhardt.xmldoclet.xjc.Interface>`


            
                |          :ref:`ClassDoc<com.sun.javadoc.ClassDoc>` classDoc

                |          returns :ref:`Class<com.github.markusbernhardt.xmldoclet.xjc.Class>`


            
                |          :ref:`ConstructorDoc<com.sun.javadoc.ConstructorDoc>` constructorDoc

                |          returns :ref:`Constructor<com.github.markusbernhardt.xmldoclet.xjc.Constructor>`


            
                |          :ref:`MethodDoc<com.sun.javadoc.MethodDoc>` methodDoc

                |          returns :ref:`Method<com.github.markusbernhardt.xmldoclet.xjc.Method>`


            
                |          :ref:`Parameter<com.sun.javadoc.Parameter>` parameter

                |          returns :ref:`MethodParameter<com.github.markusbernhardt.xmldoclet.xjc.MethodParameter>`


            
                |          :ref:`FieldDoc<com.sun.javadoc.FieldDoc>` fieldDoc

                |          returns :ref:`Field<com.github.markusbernhardt.xmldoclet.xjc.Field>`


            
                |          :ref:`Type<com.sun.javadoc.Type>` type

                |          returns :ref:`TypeInfo<com.github.markusbernhardt.xmldoclet.xjc.TypeInfo>`


            
                |          :ref:`WildcardType<com.sun.javadoc.WildcardType>` wildcard

                |          returns :ref:`Wildcard<com.github.markusbernhardt.xmldoclet.xjc.Wildcard>`


            
                Parse type variables for generics
                
                
                |          :ref:`TypeVariable<com.sun.javadoc.TypeVariable>` typeVariable

                |          returns :ref:`TypeParameter<com.github.markusbernhardt.xmldoclet.xjc.TypeParameter>`  | 


            
                |          :ref:`Tag<com.sun.javadoc.Tag>` tagDoc

                |          returns :ref:`TagInfo<com.github.markusbernhardt.xmldoclet.xjc.TagInfo>`


            
                Returns string representation of scope
                
                
                |          :ref:`ProgramElementDoc<com.sun.javadoc.ProgramElementDoc>` doc

                |          returns :ref:`String<java.lang.String>`  | 


            
..  _com.github.markusbernhardt.xmldoclet.XmlDoclet:

=======================================================================
XmlDoclet
=======================================================================

*extends:* :ref:`Object<java.lang.Object>` 

| Doclet class.

| **XmlDoclet** ()


| **optionLength** (optionName) → int
| Check for doclet-added options. Returns the number of arguments you must specify on the command line for the given option. For example, "-d docs" would return 2. 
| This method is required if the doclet contains any options. If this method is missing, Javadoc will print an invalid flag error for every option.
|          :ref:`String<java.lang.String>` optionName  | optionName The name of the option.
|          returns int  | number of arguments on the command line for an option including the option name itself. Zero return means option not known. Negative value means error occurred.



| **validOptions** (optionsArrayArray, reporter) → boolean
| Check that options have the correct arguments. 
| This method is not required, but is recommended, as every option will be considered valid if this method is not present. It will default gracefully (to true) if absent. 
| Printing option related error messages (using the provided DocErrorReporter) is the responsibility of this method.
|          :ref:`String<java.lang.String>` optionsArrayArray  | optionsArrayArray The two-dimensional array of options.
|          :ref:`DocErrorReporter<com.sun.javadoc.DocErrorReporter>` reporter  | reporter The error reporter.
|          returns boolean  | ``true`` if the options are valid.



| **start** (rootDoc) → boolean
| Processes the JavaDoc documentation. 
| This method is required for all doclets.
|          :ref:`RootDoc<com.sun.javadoc.RootDoc>` rootDoc  | rootDoc The root of the documentation tree.
|          returns boolean  | ``true`` if processing was successful.



| **transform** (xsltInputStream, xmlFile, outFile, parameters)
|          :ref:`InputStream<java.io.InputStream>` xsltInputStream
|          :ref:`File<java.io.File>` xmlFile
|          :ref:`File<java.io.File>` outFile
|          :ref:`Map<java.util.Map>` parameters


| **save** (commandLine, root)
| Save XML object model to a file via JAXB.
|          CommandLine commandLine  | commandLine the parsed command line arguments
|          :ref:`Root<com.github.markusbernhardt.xmldoclet.xjc.Root>` root  | root the document root


| **languageVersion** () → :ref:`LanguageVersion<com.sun.javadoc.LanguageVersion>`
| Return the version of the Java Programming Language supported by this doclet. 
| This method is required by any doclet supporting a language version newer than 1.1. 
| This Doclet supports Java 5.
|          returns :ref:`LanguageVersion<com.sun.javadoc.LanguageVersion>`  | LanguageVersion#JAVA_1_5



| **parseCommandLine** (optionsArrayArray) → CommandLine
| Parse the given options.
|          :ref:`String<java.lang.String>` optionsArrayArray  | optionsArrayArray The two dimensional array of options.
|          returns CommandLine  | the parsed command line arguments.




..  _com.github.markusbernhardt.xmldoclet.xjc:
***********************************************************************
xjc
***********************************************************************

..  _com.github.markusbernhardt.xmldoclet.xjc.Annotation:

=======================================================================
Annotation
=======================================================================

*extends:* :ref:`Object<java.lang.Object>` 

| 
| Java class for annotation complex type. 
| The following schema fragment specifies the expected content contained within this class. `&lt;complexType name="annotation"&gt; &lt;complexContent&gt; &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt; &lt;sequence&gt; &lt;element name="comment" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/&gt; &lt;element name="tag" type="{}tagInfo" maxOccurs="unbounded" minOccurs="0"/&gt; &lt;element name="element" type="{}annotationElement" maxOccurs="unbounded" minOccurs="0"/&gt; &lt;element name="annotation" type="{}annotationInstance" maxOccurs="unbounded" minOccurs="0"/&gt; &lt;/sequence&gt; &lt;attribute name="name" type="{http://www.w3.org/2001/XMLSchema}string" /&gt; &lt;attribute name="qualified" type="{http://www.w3.org/2001/XMLSchema}string" /&gt; &lt;attribute name="scope" type="{}scope" /&gt; &lt;attribute name="included" type="{http://www.w3.org/2001/XMLSchema}boolean" default="true" /&gt; &lt;/restriction&gt; &lt;/complexContent&gt; &lt;/complexType&gt;`

| **Annotation** ()


| **getComment** () → :ref:`String<java.lang.String>`
| Gets the value of the comment property.
|          returns :ref:`String<java.lang.String>`  | possible object is `String`



| **setComment** (value)
| Sets the value of the comment property.
|          :ref:`String<java.lang.String>` value


| **getTag** () → :ref:`List<java.util.List>`
| Gets the value of the tag property. 
| This accessor method returns a reference to the live list, not a snapshot. Therefore any modification you make to the returned list will be present inside the JAXB object. This is why there is not a ``set`` method for the tag property. 
| For example, to add a new item, do as follows: `getTag().add(newItem);` 
| Objects of the following type(s) are allowed in the list `TagInfo`
|          returns :ref:`List<java.util.List>`



| **getElement** () → :ref:`List<java.util.List>`
| Gets the value of the element property. 
| This accessor method returns a reference to the live list, not a snapshot. Therefore any modification you make to the returned list will be present inside the JAXB object. This is why there is not a ``set`` method for the element property. 
| For example, to add a new item, do as follows: `getElement().add(newItem);` 
| Objects of the following type(s) are allowed in the list `AnnotationElement`
|          returns :ref:`List<java.util.List>`



| **getAnnotation** () → :ref:`List<java.util.List>`
| Gets the value of the annotation property. 
| This accessor method returns a reference to the live list, not a snapshot. Therefore any modification you make to the returned list will be present inside the JAXB object. This is why there is not a ``set`` method for the annotation property. 
| For example, to add a new item, do as follows: `getAnnotation().add(newItem);` 
| Objects of the following type(s) are allowed in the list `AnnotationInstance`
|          returns :ref:`List<java.util.List>`



| **getName** () → :ref:`String<java.lang.String>`
| Gets the value of the name property.
|          returns :ref:`String<java.lang.String>`  | possible object is `String`



| **setName** (value)
| Sets the value of the name property.
|          :ref:`String<java.lang.String>` value


| **getQualified** () → :ref:`String<java.lang.String>`
| Gets the value of the qualified property.
|          returns :ref:`String<java.lang.String>`  | possible object is `String`



| **setQualified** (value)
| Sets the value of the qualified property.
|          :ref:`String<java.lang.String>` value


| **getScope** () → :ref:`String<java.lang.String>`
| Gets the value of the scope property.
|          returns :ref:`String<java.lang.String>`  | possible object is `String`



| **setScope** (value)
| Sets the value of the scope property.
|          :ref:`String<java.lang.String>` value


| **isIncluded** () → boolean
| Gets the value of the included property.
|          returns boolean  | possible object is `Boolean`



| **setIncluded** (value)
| Sets the value of the included property.
|          :ref:`Boolean<java.lang.Boolean>` value



..  _com.github.markusbernhardt.xmldoclet.xjc.AnnotationArgument:

=======================================================================
AnnotationArgument
=======================================================================

*extends:* :ref:`Object<java.lang.Object>` 

| 
| Java class for annotationArgument complex type. 
| The following schema fragment specifies the expected content contained within this class. `&lt;complexType name="annotationArgument"&gt; &lt;complexContent&gt; &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt; &lt;sequence&gt; &lt;element name="type" type="{}typeInfo" minOccurs="0"/&gt; &lt;choice&gt; &lt;element name="value" type="{http://www.w3.org/2001/XMLSchema}string" maxOccurs="unbounded" minOccurs="0"/&gt; &lt;element name="annotation" type="{}annotationInstance" maxOccurs="unbounded" minOccurs="0"/&gt; &lt;/choice&gt; &lt;/sequence&gt; &lt;attribute name="name" type="{http://www.w3.org/2001/XMLSchema}string" /&gt; &lt;attribute name="primitive" type="{http://www.w3.org/2001/XMLSchema}boolean" default="false" /&gt; &lt;attribute name="array" type="{http://www.w3.org/2001/XMLSchema}boolean" default="false" /&gt; &lt;/restriction&gt; &lt;/complexContent&gt; &lt;/complexType&gt;`

| **AnnotationArgument** ()


| **getType** () → :ref:`TypeInfo<com.github.markusbernhardt.xmldoclet.xjc.TypeInfo>`
| Gets the value of the type property.
|          returns :ref:`TypeInfo<com.github.markusbernhardt.xmldoclet.xjc.TypeInfo>`  | possible object is `TypeInfo`



| **setType** (value)
| Sets the value of the type property.
|          :ref:`TypeInfo<com.github.markusbernhardt.xmldoclet.xjc.TypeInfo>` value


| **getValue** () → :ref:`List<java.util.List>`
| Gets the value of the value property. 
| This accessor method returns a reference to the live list, not a snapshot. Therefore any modification you make to the returned list will be present inside the JAXB object. This is why there is not a ``set`` method for the value property. 
| For example, to add a new item, do as follows: `getValue().add(newItem);` 
| Objects of the following type(s) are allowed in the list `String`
|          returns :ref:`List<java.util.List>`



| **getAnnotation** () → :ref:`List<java.util.List>`
| Gets the value of the annotation property. 
| This accessor method returns a reference to the live list, not a snapshot. Therefore any modification you make to the returned list will be present inside the JAXB object. This is why there is not a ``set`` method for the annotation property. 
| For example, to add a new item, do as follows: `getAnnotation().add(newItem);` 
| Objects of the following type(s) are allowed in the list `AnnotationInstance`
|          returns :ref:`List<java.util.List>`



| **getName** () → :ref:`String<java.lang.String>`
| Gets the value of the name property.
|          returns :ref:`String<java.lang.String>`  | possible object is `String`



| **setName** (value)
| Sets the value of the name property.
|          :ref:`String<java.lang.String>` value


| **isPrimitive** () → boolean
| Gets the value of the primitive property.
|          returns boolean  | possible object is `Boolean`



| **setPrimitive** (value)
| Sets the value of the primitive property.
|          :ref:`Boolean<java.lang.Boolean>` value


| **isArray** () → boolean
| Gets the value of the array property.
|          returns boolean  | possible object is `Boolean`



| **setArray** (value)
| Sets the value of the array property.
|          :ref:`Boolean<java.lang.Boolean>` value



..  _com.github.markusbernhardt.xmldoclet.xjc.AnnotationElement:

=======================================================================
AnnotationElement
=======================================================================

*extends:* :ref:`Object<java.lang.Object>` 

| 
| Java class for annotationElement complex type. 
| The following schema fragment specifies the expected content contained within this class. `&lt;complexType name="annotationElement"&gt; &lt;complexContent&gt; &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt; &lt;sequence&gt; &lt;element name="type" type="{}typeInfo" minOccurs="0"/&gt; &lt;/sequence&gt; &lt;attribute name="name" type="{http://www.w3.org/2001/XMLSchema}string" /&gt; &lt;attribute name="qualified" type="{http://www.w3.org/2001/XMLSchema}string" /&gt; &lt;attribute name="default" type="{http://www.w3.org/2001/XMLSchema}string" /&gt; &lt;/restriction&gt; &lt;/complexContent&gt; &lt;/complexType&gt;`

| **AnnotationElement** ()


| **getType** () → :ref:`TypeInfo<com.github.markusbernhardt.xmldoclet.xjc.TypeInfo>`
| Gets the value of the type property.
|          returns :ref:`TypeInfo<com.github.markusbernhardt.xmldoclet.xjc.TypeInfo>`  | possible object is `TypeInfo`



| **setType** (value)
| Sets the value of the type property.
|          :ref:`TypeInfo<com.github.markusbernhardt.xmldoclet.xjc.TypeInfo>` value


| **getName** () → :ref:`String<java.lang.String>`
| Gets the value of the name property.
|          returns :ref:`String<java.lang.String>`  | possible object is `String`



| **setName** (value)
| Sets the value of the name property.
|          :ref:`String<java.lang.String>` value


| **getQualified** () → :ref:`String<java.lang.String>`
| Gets the value of the qualified property.
|          returns :ref:`String<java.lang.String>`  | possible object is `String`



| **setQualified** (value)
| Sets the value of the qualified property.
|          :ref:`String<java.lang.String>` value


| **getDefault** () → :ref:`String<java.lang.String>`
| Gets the value of the default property.
|          returns :ref:`String<java.lang.String>`  | possible object is `String`



| **setDefault** (value)
| Sets the value of the default property.
|          :ref:`String<java.lang.String>` value



..  _com.github.markusbernhardt.xmldoclet.xjc.AnnotationInstance:

=======================================================================
AnnotationInstance
=======================================================================

*extends:* :ref:`Object<java.lang.Object>` 

| 
| Java class for annotationInstance complex type. 
| The following schema fragment specifies the expected content contained within this class. `&lt;complexType name="annotationInstance"&gt; &lt;complexContent&gt; &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt; &lt;sequence&gt; &lt;element name="argument" type="{}annotationArgument" maxOccurs="unbounded" minOccurs="0"/&gt; &lt;/sequence&gt; &lt;attribute name="name" type="{http://www.w3.org/2001/XMLSchema}string" /&gt; &lt;attribute name="qualified" type="{http://www.w3.org/2001/XMLSchema}string" /&gt; &lt;/restriction&gt; &lt;/complexContent&gt; &lt;/complexType&gt;`

| **AnnotationInstance** ()


| **getArgument** () → :ref:`List<java.util.List>`
| Gets the value of the argument property. 
| This accessor method returns a reference to the live list, not a snapshot. Therefore any modification you make to the returned list will be present inside the JAXB object. This is why there is not a ``set`` method for the argument property. 
| For example, to add a new item, do as follows: `getArgument().add(newItem);` 
| Objects of the following type(s) are allowed in the list `AnnotationArgument`
|          returns :ref:`List<java.util.List>`



| **getName** () → :ref:`String<java.lang.String>`
| Gets the value of the name property.
|          returns :ref:`String<java.lang.String>`  | possible object is `String`



| **setName** (value)
| Sets the value of the name property.
|          :ref:`String<java.lang.String>` value


| **getQualified** () → :ref:`String<java.lang.String>`
| Gets the value of the qualified property.
|          returns :ref:`String<java.lang.String>`  | possible object is `String`



| **setQualified** (value)
| Sets the value of the qualified property.
|          :ref:`String<java.lang.String>` value



..  _com.github.markusbernhardt.xmldoclet.xjc.Class:

=======================================================================
Class
=======================================================================

*extends:* :ref:`Object<java.lang.Object>` 

| 
| Java class for class complex type. 
| The following schema fragment specifies the expected content contained within this class. `&lt;complexType name="class"&gt; &lt;complexContent&gt; &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt; &lt;sequence&gt; &lt;element name="comment" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/&gt; &lt;element name="tag" type="{}tagInfo" maxOccurs="unbounded" minOccurs="0"/&gt; &lt;element name="generic" type="{}typeParameter" maxOccurs="unbounded" minOccurs="0"/&gt; &lt;element name="class" type="{}typeInfo" minOccurs="0"/&gt; &lt;element name="interface" type="{}typeInfo" maxOccurs="unbounded" minOccurs="0"/&gt; &lt;element name="constructor" type="{}constructor" maxOccurs="unbounded" minOccurs="0"/&gt; &lt;element name="method" type="{}method" maxOccurs="unbounded" minOccurs="0"/&gt; &lt;element name="annotation" type="{}annotationInstance" maxOccurs="unbounded" minOccurs="0"/&gt; &lt;element name="field" type="{}field" maxOccurs="unbounded" minOccurs="0"/&gt; &lt;/sequence&gt; &lt;attribute name="name" type="{http://www.w3.org/2001/XMLSchema}string" /&gt; &lt;attribute name="qualified" type="{http://www.w3.org/2001/XMLSchema}string" /&gt; &lt;attribute name="scope" type="{}scope" /&gt; &lt;attribute name="abstract" type="{http://www.w3.org/2001/XMLSchema}boolean" default="false" /&gt; &lt;attribute name="error" type="{http://www.w3.org/2001/XMLSchema}boolean" default="false" /&gt; &lt;attribute name="exception" type="{http://www.w3.org/2001/XMLSchema}boolean" default="false" /&gt; &lt;attribute name="externalizable" type="{http://www.w3.org/2001/XMLSchema}boolean" default="false" /&gt; &lt;attribute name="included" type="{http://www.w3.org/2001/XMLSchema}boolean" default="true" /&gt; &lt;attribute name="serializable" type="{http://www.w3.org/2001/XMLSchema}boolean" default="false" /&gt; &lt;/restriction&gt; &lt;/complexContent&gt; &lt;/complexType&gt;`

| **Class** ()


| **getComment** () → :ref:`String<java.lang.String>`
| Gets the value of the comment property.
|          returns :ref:`String<java.lang.String>`  | possible object is `String`



| **setComment** (value)
| Sets the value of the comment property.
|          :ref:`String<java.lang.String>` value


| **getTag** () → :ref:`List<java.util.List>`
| Gets the value of the tag property. 
| This accessor method returns a reference to the live list, not a snapshot. Therefore any modification you make to the returned list will be present inside the JAXB object. This is why there is not a ``set`` method for the tag property. 
| For example, to add a new item, do as follows: `getTag().add(newItem);` 
| Objects of the following type(s) are allowed in the list `TagInfo`
|          returns :ref:`List<java.util.List>`



| **getGeneric** () → :ref:`List<java.util.List>`
| Gets the value of the generic property. 
| This accessor method returns a reference to the live list, not a snapshot. Therefore any modification you make to the returned list will be present inside the JAXB object. This is why there is not a ``set`` method for the generic property. 
| For example, to add a new item, do as follows: `getGeneric().add(newItem);` 
| Objects of the following type(s) are allowed in the list `TypeParameter`
|          returns :ref:`List<java.util.List>`



| **getClazz** () → :ref:`TypeInfo<com.github.markusbernhardt.xmldoclet.xjc.TypeInfo>`
| Gets the value of the clazz property.
|          returns :ref:`TypeInfo<com.github.markusbernhardt.xmldoclet.xjc.TypeInfo>`  | possible object is `TypeInfo`



| **setClazz** (value)
| Sets the value of the clazz property.
|          :ref:`TypeInfo<com.github.markusbernhardt.xmldoclet.xjc.TypeInfo>` value


| **getInterface** () → :ref:`List<java.util.List>`
| Gets the value of the interface property. 
| This accessor method returns a reference to the live list, not a snapshot. Therefore any modification you make to the returned list will be present inside the JAXB object. This is why there is not a ``set`` method for the interface property. 
| For example, to add a new item, do as follows: `getInterface().add(newItem);` 
| Objects of the following type(s) are allowed in the list `TypeInfo`
|          returns :ref:`List<java.util.List>`



| **getConstructor** () → :ref:`List<java.util.List>`
| Gets the value of the constructor property. 
| This accessor method returns a reference to the live list, not a snapshot. Therefore any modification you make to the returned list will be present inside the JAXB object. This is why there is not a ``set`` method for the constructor property. 
| For example, to add a new item, do as follows: `getConstructor().add(newItem);` 
| Objects of the following type(s) are allowed in the list `Constructor`
|          returns :ref:`List<java.util.List>`



| **getMethod** () → :ref:`List<java.util.List>`
| Gets the value of the method property. 
| This accessor method returns a reference to the live list, not a snapshot. Therefore any modification you make to the returned list will be present inside the JAXB object. This is why there is not a ``set`` method for the method property. 
| For example, to add a new item, do as follows: `getMethod().add(newItem);` 
| Objects of the following type(s) are allowed in the list `Method`
|          returns :ref:`List<java.util.List>`



| **getAnnotation** () → :ref:`List<java.util.List>`
| Gets the value of the annotation property. 
| This accessor method returns a reference to the live list, not a snapshot. Therefore any modification you make to the returned list will be present inside the JAXB object. This is why there is not a ``set`` method for the annotation property. 
| For example, to add a new item, do as follows: `getAnnotation().add(newItem);` 
| Objects of the following type(s) are allowed in the list `AnnotationInstance`
|          returns :ref:`List<java.util.List>`



| **getField** () → :ref:`List<java.util.List>`
| Gets the value of the field property. 
| This accessor method returns a reference to the live list, not a snapshot. Therefore any modification you make to the returned list will be present inside the JAXB object. This is why there is not a ``set`` method for the field property. 
| For example, to add a new item, do as follows: `getField().add(newItem);` 
| Objects of the following type(s) are allowed in the list `Field`
|          returns :ref:`List<java.util.List>`



| **getName** () → :ref:`String<java.lang.String>`
| Gets the value of the name property.
|          returns :ref:`String<java.lang.String>`  | possible object is `String`



| **setName** (value)
| Sets the value of the name property.
|          :ref:`String<java.lang.String>` value


| **getQualified** () → :ref:`String<java.lang.String>`
| Gets the value of the qualified property.
|          returns :ref:`String<java.lang.String>`  | possible object is `String`



| **setQualified** (value)
| Sets the value of the qualified property.
|          :ref:`String<java.lang.String>` value


| **getScope** () → :ref:`String<java.lang.String>`
| Gets the value of the scope property.
|          returns :ref:`String<java.lang.String>`  | possible object is `String`



| **setScope** (value)
| Sets the value of the scope property.
|          :ref:`String<java.lang.String>` value


| **isAbstract** () → boolean
| Gets the value of the abstract property.
|          returns boolean  | possible object is `Boolean`



| **setAbstract** (value)
| Sets the value of the abstract property.
|          :ref:`Boolean<java.lang.Boolean>` value


| **isError** () → boolean
| Gets the value of the error property.
|          returns boolean  | possible object is `Boolean`



| **setError** (value)
| Sets the value of the error property.
|          :ref:`Boolean<java.lang.Boolean>` value


| **isException** () → boolean
| Gets the value of the exception property.
|          returns boolean  | possible object is `Boolean`



| **setException** (value)
| Sets the value of the exception property.
|          :ref:`Boolean<java.lang.Boolean>` value


| **isExternalizable** () → boolean
| Gets the value of the externalizable property.
|          returns boolean  | possible object is `Boolean`



| **setExternalizable** (value)
| Sets the value of the externalizable property.
|          :ref:`Boolean<java.lang.Boolean>` value


| **isIncluded** () → boolean
| Gets the value of the included property.
|          returns boolean  | possible object is `Boolean`



| **setIncluded** (value)
| Sets the value of the included property.
|          :ref:`Boolean<java.lang.Boolean>` value


| **isSerializable** () → boolean
| Gets the value of the serializable property.
|          returns boolean  | possible object is `Boolean`



| **setSerializable** (value)
| Sets the value of the serializable property.
|          :ref:`Boolean<java.lang.Boolean>` value



..  _com.github.markusbernhardt.xmldoclet.xjc.Constructor:

=======================================================================
Constructor
=======================================================================

*extends:* :ref:`Object<java.lang.Object>` 

| 
| Java class for constructor complex type. 
| The following schema fragment specifies the expected content contained within this class. `&lt;complexType name="constructor"&gt; &lt;complexContent&gt; &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt; &lt;sequence&gt; &lt;element name="comment" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/&gt; &lt;element name="tag" type="{}tagInfo" maxOccurs="unbounded" minOccurs="0"/&gt; &lt;element name="parameter" type="{}methodParameter" maxOccurs="unbounded" minOccurs="0"/&gt; &lt;element name="exception" type="{}typeInfo" maxOccurs="unbounded" minOccurs="0"/&gt; &lt;element name="annotation" type="{}annotationInstance" maxOccurs="unbounded" minOccurs="0"/&gt; &lt;/sequence&gt; &lt;attribute name="name" type="{http://www.w3.org/2001/XMLSchema}string" /&gt; &lt;attribute name="signature" type="{http://www.w3.org/2001/XMLSchema}string" /&gt; &lt;attribute name="qualified" type="{http://www.w3.org/2001/XMLSchema}string" /&gt; &lt;attribute name="scope" type="{}scope" /&gt; &lt;attribute name="final" type="{http://www.w3.org/2001/XMLSchema}boolean" default="false" /&gt; &lt;attribute name="included" type="{http://www.w3.org/2001/XMLSchema}boolean" default="true" /&gt; &lt;attribute name="native" type="{http://www.w3.org/2001/XMLSchema}boolean" default="false" /&gt; &lt;attribute name="synchronized" type="{http://www.w3.org/2001/XMLSchema}boolean" default="false" /&gt; &lt;attribute name="static" type="{http://www.w3.org/2001/XMLSchema}boolean" default="false" /&gt; &lt;attribute name="varArgs" type="{http://www.w3.org/2001/XMLSchema}boolean" default="false" /&gt; &lt;/restriction&gt; &lt;/complexContent&gt; &lt;/complexType&gt;`

| **Constructor** ()


| **getComment** () → :ref:`String<java.lang.String>`
| Gets the value of the comment property.
|          returns :ref:`String<java.lang.String>`  | possible object is `String`



| **setComment** (value)
| Sets the value of the comment property.
|          :ref:`String<java.lang.String>` value


| **getTag** () → :ref:`List<java.util.List>`
| Gets the value of the tag property. 
| This accessor method returns a reference to the live list, not a snapshot. Therefore any modification you make to the returned list will be present inside the JAXB object. This is why there is not a ``set`` method for the tag property. 
| For example, to add a new item, do as follows: `getTag().add(newItem);` 
| Objects of the following type(s) are allowed in the list `TagInfo`
|          returns :ref:`List<java.util.List>`



| **getParameter** () → :ref:`List<java.util.List>`
| Gets the value of the parameter property. 
| This accessor method returns a reference to the live list, not a snapshot. Therefore any modification you make to the returned list will be present inside the JAXB object. This is why there is not a ``set`` method for the parameter property. 
| For example, to add a new item, do as follows: `getParameter().add(newItem);` 
| Objects of the following type(s) are allowed in the list `MethodParameter`
|          returns :ref:`List<java.util.List>`



| **getException** () → :ref:`List<java.util.List>`
| Gets the value of the exception property. 
| This accessor method returns a reference to the live list, not a snapshot. Therefore any modification you make to the returned list will be present inside the JAXB object. This is why there is not a ``set`` method for the exception property. 
| For example, to add a new item, do as follows: `getException().add(newItem);` 
| Objects of the following type(s) are allowed in the list `TypeInfo`
|          returns :ref:`List<java.util.List>`



| **getAnnotation** () → :ref:`List<java.util.List>`
| Gets the value of the annotation property. 
| This accessor method returns a reference to the live list, not a snapshot. Therefore any modification you make to the returned list will be present inside the JAXB object. This is why there is not a ``set`` method for the annotation property. 
| For example, to add a new item, do as follows: `getAnnotation().add(newItem);` 
| Objects of the following type(s) are allowed in the list `AnnotationInstance`
|          returns :ref:`List<java.util.List>`



| **getName** () → :ref:`String<java.lang.String>`
| Gets the value of the name property.
|          returns :ref:`String<java.lang.String>`  | possible object is `String`



| **setName** (value)
| Sets the value of the name property.
|          :ref:`String<java.lang.String>` value


| **getSignature** () → :ref:`String<java.lang.String>`
| Gets the value of the signature property.
|          returns :ref:`String<java.lang.String>`  | possible object is `String`



| **setSignature** (value)
| Sets the value of the signature property.
|          :ref:`String<java.lang.String>` value


| **getQualified** () → :ref:`String<java.lang.String>`
| Gets the value of the qualified property.
|          returns :ref:`String<java.lang.String>`  | possible object is `String`



| **setQualified** (value)
| Sets the value of the qualified property.
|          :ref:`String<java.lang.String>` value


| **getScope** () → :ref:`String<java.lang.String>`
| Gets the value of the scope property.
|          returns :ref:`String<java.lang.String>`  | possible object is `String`



| **setScope** (value)
| Sets the value of the scope property.
|          :ref:`String<java.lang.String>` value


| **isFinal** () → boolean
| Gets the value of the final property.
|          returns boolean  | possible object is `Boolean`



| **setFinal** (value)
| Sets the value of the final property.
|          :ref:`Boolean<java.lang.Boolean>` value


| **isIncluded** () → boolean
| Gets the value of the included property.
|          returns boolean  | possible object is `Boolean`



| **setIncluded** (value)
| Sets the value of the included property.
|          :ref:`Boolean<java.lang.Boolean>` value


| **isNative** () → boolean
| Gets the value of the native property.
|          returns boolean  | possible object is `Boolean`



| **setNative** (value)
| Sets the value of the native property.
|          :ref:`Boolean<java.lang.Boolean>` value


| **isSynchronized** () → boolean
| Gets the value of the synchronized property.
|          returns boolean  | possible object is `Boolean`



| **setSynchronized** (value)
| Sets the value of the synchronized property.
|          :ref:`Boolean<java.lang.Boolean>` value


| **isStatic** () → boolean
| Gets the value of the static property.
|          returns boolean  | possible object is `Boolean`



| **setStatic** (value)
| Sets the value of the static property.
|          :ref:`Boolean<java.lang.Boolean>` value


| **isVarArgs** () → boolean
| Gets the value of the varArgs property.
|          returns boolean  | possible object is `Boolean`



| **setVarArgs** (value)
| Sets the value of the varArgs property.
|          :ref:`Boolean<java.lang.Boolean>` value



..  _com.github.markusbernhardt.xmldoclet.xjc.Enum:

=======================================================================
Enum
=======================================================================

*extends:* :ref:`Object<java.lang.Object>` 

| 
| Java class for enum complex type. 
| The following schema fragment specifies the expected content contained within this class. `&lt;complexType name="enum"&gt; &lt;complexContent&gt; &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt; &lt;sequence&gt; &lt;element name="comment" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/&gt; &lt;element name="tag" type="{}tagInfo" maxOccurs="unbounded" minOccurs="0"/&gt; &lt;element name="class" type="{}typeInfo" minOccurs="0"/&gt; &lt;element name="interface" type="{}typeInfo" maxOccurs="unbounded" minOccurs="0"/&gt; &lt;element name="constant" type="{}enumConstant" maxOccurs="unbounded" minOccurs="0"/&gt; &lt;element name="annotation" type="{}annotationInstance" maxOccurs="unbounded" minOccurs="0"/&gt; &lt;/sequence&gt; &lt;attribute name="name" type="{http://www.w3.org/2001/XMLSchema}string" /&gt; &lt;attribute name="qualified" type="{http://www.w3.org/2001/XMLSchema}string" /&gt; &lt;attribute name="scope" type="{}scope" /&gt; &lt;attribute name="included" type="{http://www.w3.org/2001/XMLSchema}boolean" default="true" /&gt; &lt;/restriction&gt; &lt;/complexContent&gt; &lt;/complexType&gt;`

| **Enum** ()


| **getComment** () → :ref:`String<java.lang.String>`
| Gets the value of the comment property.
|          returns :ref:`String<java.lang.String>`  | possible object is `String`



| **setComment** (value)
| Sets the value of the comment property.
|          :ref:`String<java.lang.String>` value


| **getTag** () → :ref:`List<java.util.List>`
| Gets the value of the tag property. 
| This accessor method returns a reference to the live list, not a snapshot. Therefore any modification you make to the returned list will be present inside the JAXB object. This is why there is not a ``set`` method for the tag property. 
| For example, to add a new item, do as follows: `getTag().add(newItem);` 
| Objects of the following type(s) are allowed in the list `TagInfo`
|          returns :ref:`List<java.util.List>`



| **getClazz** () → :ref:`TypeInfo<com.github.markusbernhardt.xmldoclet.xjc.TypeInfo>`
| Gets the value of the clazz property.
|          returns :ref:`TypeInfo<com.github.markusbernhardt.xmldoclet.xjc.TypeInfo>`  | possible object is `TypeInfo`



| **setClazz** (value)
| Sets the value of the clazz property.
|          :ref:`TypeInfo<com.github.markusbernhardt.xmldoclet.xjc.TypeInfo>` value


| **getInterface** () → :ref:`List<java.util.List>`
| Gets the value of the interface property. 
| This accessor method returns a reference to the live list, not a snapshot. Therefore any modification you make to the returned list will be present inside the JAXB object. This is why there is not a ``set`` method for the interface property. 
| For example, to add a new item, do as follows: `getInterface().add(newItem);` 
| Objects of the following type(s) are allowed in the list `TypeInfo`
|          returns :ref:`List<java.util.List>`



| **getConstant** () → :ref:`List<java.util.List>`
| Gets the value of the constant property. 
| This accessor method returns a reference to the live list, not a snapshot. Therefore any modification you make to the returned list will be present inside the JAXB object. This is why there is not a ``set`` method for the constant property. 
| For example, to add a new item, do as follows: `getConstant().add(newItem);` 
| Objects of the following type(s) are allowed in the list `EnumConstant`
|          returns :ref:`List<java.util.List>`



| **getAnnotation** () → :ref:`List<java.util.List>`
| Gets the value of the annotation property. 
| This accessor method returns a reference to the live list, not a snapshot. Therefore any modification you make to the returned list will be present inside the JAXB object. This is why there is not a ``set`` method for the annotation property. 
| For example, to add a new item, do as follows: `getAnnotation().add(newItem);` 
| Objects of the following type(s) are allowed in the list `AnnotationInstance`
|          returns :ref:`List<java.util.List>`



| **getName** () → :ref:`String<java.lang.String>`
| Gets the value of the name property.
|          returns :ref:`String<java.lang.String>`  | possible object is `String`



| **setName** (value)
| Sets the value of the name property.
|          :ref:`String<java.lang.String>` value


| **getQualified** () → :ref:`String<java.lang.String>`
| Gets the value of the qualified property.
|          returns :ref:`String<java.lang.String>`  | possible object is `String`



| **setQualified** (value)
| Sets the value of the qualified property.
|          :ref:`String<java.lang.String>` value


| **getScope** () → :ref:`String<java.lang.String>`
| Gets the value of the scope property.
|          returns :ref:`String<java.lang.String>`  | possible object is `String`



| **setScope** (value)
| Sets the value of the scope property.
|          :ref:`String<java.lang.String>` value


| **isIncluded** () → boolean
| Gets the value of the included property.
|          returns boolean  | possible object is `Boolean`



| **setIncluded** (value)
| Sets the value of the included property.
|          :ref:`Boolean<java.lang.Boolean>` value



..  _com.github.markusbernhardt.xmldoclet.xjc.EnumConstant:

=======================================================================
EnumConstant
=======================================================================

*extends:* :ref:`Object<java.lang.Object>` 

| 
| Java class for enumConstant complex type. 
| The following schema fragment specifies the expected content contained within this class. `&lt;complexType name="enumConstant"&gt; &lt;complexContent&gt; &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt; &lt;sequence&gt; &lt;element name="comment" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/&gt; &lt;element name="tag" type="{}tagInfo" maxOccurs="unbounded" minOccurs="0"/&gt; &lt;element name="annotation" type="{}annotationInstance" maxOccurs="unbounded" minOccurs="0"/&gt; &lt;/sequence&gt; &lt;attribute name="name" type="{http://www.w3.org/2001/XMLSchema}string" /&gt; &lt;/restriction&gt; &lt;/complexContent&gt; &lt;/complexType&gt;`

| **EnumConstant** ()


| **getComment** () → :ref:`String<java.lang.String>`
| Gets the value of the comment property.
|          returns :ref:`String<java.lang.String>`  | possible object is `String`



| **setComment** (value)
| Sets the value of the comment property.
|          :ref:`String<java.lang.String>` value


| **getTag** () → :ref:`List<java.util.List>`
| Gets the value of the tag property. 
| This accessor method returns a reference to the live list, not a snapshot. Therefore any modification you make to the returned list will be present inside the JAXB object. This is why there is not a ``set`` method for the tag property. 
| For example, to add a new item, do as follows: `getTag().add(newItem);` 
| Objects of the following type(s) are allowed in the list `TagInfo`
|          returns :ref:`List<java.util.List>`



| **getAnnotation** () → :ref:`List<java.util.List>`
| Gets the value of the annotation property. 
| This accessor method returns a reference to the live list, not a snapshot. Therefore any modification you make to the returned list will be present inside the JAXB object. This is why there is not a ``set`` method for the annotation property. 
| For example, to add a new item, do as follows: `getAnnotation().add(newItem);` 
| Objects of the following type(s) are allowed in the list `AnnotationInstance`
|          returns :ref:`List<java.util.List>`



| **getName** () → :ref:`String<java.lang.String>`
| Gets the value of the name property.
|          returns :ref:`String<java.lang.String>`  | possible object is `String`



| **setName** (value)
| Sets the value of the name property.
|          :ref:`String<java.lang.String>` value



..  _com.github.markusbernhardt.xmldoclet.xjc.Field:

=======================================================================
Field
=======================================================================

*extends:* :ref:`Object<java.lang.Object>` 

| 
| Java class for field complex type. 
| The following schema fragment specifies the expected content contained within this class. `&lt;complexType name="field"&gt; &lt;complexContent&gt; &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt; &lt;sequence&gt; &lt;element name="type" type="{}typeInfo" minOccurs="0"/&gt; &lt;element name="comment" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/&gt; &lt;element name="tag" type="{}tagInfo" maxOccurs="unbounded" minOccurs="0"/&gt; &lt;element name="constant" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/&gt; &lt;element name="annotation" type="{}annotationInstance" maxOccurs="unbounded" minOccurs="0"/&gt; &lt;/sequence&gt; &lt;attribute name="name" type="{http://www.w3.org/2001/XMLSchema}string" /&gt; &lt;attribute name="qualified" type="{http://www.w3.org/2001/XMLSchema}string" /&gt; &lt;attribute name="scope" type="{}scope" /&gt; &lt;attribute name="volatile" type="{http://www.w3.org/2001/XMLSchema}boolean" default="false" /&gt; &lt;attribute name="transient" type="{http://www.w3.org/2001/XMLSchema}boolean" default="false" /&gt; &lt;attribute name="static" type="{http://www.w3.org/2001/XMLSchema}boolean" default="false" /&gt; &lt;attribute name="final" type="{http://www.w3.org/2001/XMLSchema}boolean" default="false" /&gt; &lt;/restriction&gt; &lt;/complexContent&gt; &lt;/complexType&gt;`

| **Field** ()


| **getType** () → :ref:`TypeInfo<com.github.markusbernhardt.xmldoclet.xjc.TypeInfo>`
| Gets the value of the type property.
|          returns :ref:`TypeInfo<com.github.markusbernhardt.xmldoclet.xjc.TypeInfo>`  | possible object is `TypeInfo`



| **setType** (value)
| Sets the value of the type property.
|          :ref:`TypeInfo<com.github.markusbernhardt.xmldoclet.xjc.TypeInfo>` value


| **getComment** () → :ref:`String<java.lang.String>`
| Gets the value of the comment property.
|          returns :ref:`String<java.lang.String>`  | possible object is `String`



| **setComment** (value)
| Sets the value of the comment property.
|          :ref:`String<java.lang.String>` value


| **getTag** () → :ref:`List<java.util.List>`
| Gets the value of the tag property. 
| This accessor method returns a reference to the live list, not a snapshot. Therefore any modification you make to the returned list will be present inside the JAXB object. This is why there is not a ``set`` method for the tag property. 
| For example, to add a new item, do as follows: `getTag().add(newItem);` 
| Objects of the following type(s) are allowed in the list `TagInfo`
|          returns :ref:`List<java.util.List>`



| **getConstant** () → :ref:`String<java.lang.String>`
| Gets the value of the constant property.
|          returns :ref:`String<java.lang.String>`  | possible object is `String`



| **setConstant** (value)
| Sets the value of the constant property.
|          :ref:`String<java.lang.String>` value


| **getAnnotation** () → :ref:`List<java.util.List>`
| Gets the value of the annotation property. 
| This accessor method returns a reference to the live list, not a snapshot. Therefore any modification you make to the returned list will be present inside the JAXB object. This is why there is not a ``set`` method for the annotation property. 
| For example, to add a new item, do as follows: `getAnnotation().add(newItem);` 
| Objects of the following type(s) are allowed in the list `AnnotationInstance`
|          returns :ref:`List<java.util.List>`



| **getName** () → :ref:`String<java.lang.String>`
| Gets the value of the name property.
|          returns :ref:`String<java.lang.String>`  | possible object is `String`



| **setName** (value)
| Sets the value of the name property.
|          :ref:`String<java.lang.String>` value


| **getQualified** () → :ref:`String<java.lang.String>`
| Gets the value of the qualified property.
|          returns :ref:`String<java.lang.String>`  | possible object is `String`



| **setQualified** (value)
| Sets the value of the qualified property.
|          :ref:`String<java.lang.String>` value


| **getScope** () → :ref:`String<java.lang.String>`
| Gets the value of the scope property.
|          returns :ref:`String<java.lang.String>`  | possible object is `String`



| **setScope** (value)
| Sets the value of the scope property.
|          :ref:`String<java.lang.String>` value


| **isVolatile** () → boolean
| Gets the value of the volatile property.
|          returns boolean  | possible object is `Boolean`



| **setVolatile** (value)
| Sets the value of the volatile property.
|          :ref:`Boolean<java.lang.Boolean>` value


| **isTransient** () → boolean
| Gets the value of the transient property.
|          returns boolean  | possible object is `Boolean`



| **setTransient** (value)
| Sets the value of the transient property.
|          :ref:`Boolean<java.lang.Boolean>` value


| **isStatic** () → boolean
| Gets the value of the static property.
|          returns boolean  | possible object is `Boolean`



| **setStatic** (value)
| Sets the value of the static property.
|          :ref:`Boolean<java.lang.Boolean>` value


| **isFinal** () → boolean
| Gets the value of the final property.
|          returns boolean  | possible object is `Boolean`



| **setFinal** (value)
| Sets the value of the final property.
|          :ref:`Boolean<java.lang.Boolean>` value



..  _com.github.markusbernhardt.xmldoclet.xjc.Interface:

=======================================================================
Interface
=======================================================================

*extends:* :ref:`Object<java.lang.Object>` 

| 
| Java class for interface complex type. 
| The following schema fragment specifies the expected content contained within this class. `&lt;complexType name="interface"&gt; &lt;complexContent&gt; &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt; &lt;sequence&gt; &lt;element name="comment" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/&gt; &lt;element name="tag" type="{}tagInfo" maxOccurs="unbounded" minOccurs="0"/&gt; &lt;element name="generic" type="{}typeParameter" maxOccurs="unbounded" minOccurs="0"/&gt; &lt;element name="interface" type="{}typeInfo" maxOccurs="unbounded" minOccurs="0"/&gt; &lt;element name="method" type="{}method" maxOccurs="unbounded" minOccurs="0"/&gt; &lt;element name="annotation" type="{}annotationInstance" maxOccurs="unbounded" minOccurs="0"/&gt; &lt;element name="field" type="{}field" maxOccurs="unbounded" minOccurs="0"/&gt; &lt;/sequence&gt; &lt;attribute name="name" type="{http://www.w3.org/2001/XMLSchema}string" /&gt; &lt;attribute name="qualified" type="{http://www.w3.org/2001/XMLSchema}string" /&gt; &lt;attribute name="scope" type="{}scope" /&gt; &lt;attribute name="included" type="{http://www.w3.org/2001/XMLSchema}boolean" default="true" /&gt; &lt;/restriction&gt; &lt;/complexContent&gt; &lt;/complexType&gt;`

| **Interface** ()


| **getComment** () → :ref:`String<java.lang.String>`
| Gets the value of the comment property.
|          returns :ref:`String<java.lang.String>`  | possible object is `String`



| **setComment** (value)
| Sets the value of the comment property.
|          :ref:`String<java.lang.String>` value


| **getTag** () → :ref:`List<java.util.List>`
| Gets the value of the tag property. 
| This accessor method returns a reference to the live list, not a snapshot. Therefore any modification you make to the returned list will be present inside the JAXB object. This is why there is not a ``set`` method for the tag property. 
| For example, to add a new item, do as follows: `getTag().add(newItem);` 
| Objects of the following type(s) are allowed in the list `TagInfo`
|          returns :ref:`List<java.util.List>`



| **getGeneric** () → :ref:`List<java.util.List>`
| Gets the value of the generic property. 
| This accessor method returns a reference to the live list, not a snapshot. Therefore any modification you make to the returned list will be present inside the JAXB object. This is why there is not a ``set`` method for the generic property. 
| For example, to add a new item, do as follows: `getGeneric().add(newItem);` 
| Objects of the following type(s) are allowed in the list `TypeParameter`
|          returns :ref:`List<java.util.List>`



| **getInterface** () → :ref:`List<java.util.List>`
| Gets the value of the interface property. 
| This accessor method returns a reference to the live list, not a snapshot. Therefore any modification you make to the returned list will be present inside the JAXB object. This is why there is not a ``set`` method for the interface property. 
| For example, to add a new item, do as follows: `getInterface().add(newItem);` 
| Objects of the following type(s) are allowed in the list `TypeInfo`
|          returns :ref:`List<java.util.List>`



| **getMethod** () → :ref:`List<java.util.List>`
| Gets the value of the method property. 
| This accessor method returns a reference to the live list, not a snapshot. Therefore any modification you make to the returned list will be present inside the JAXB object. This is why there is not a ``set`` method for the method property. 
| For example, to add a new item, do as follows: `getMethod().add(newItem);` 
| Objects of the following type(s) are allowed in the list `Method`
|          returns :ref:`List<java.util.List>`



| **getAnnotation** () → :ref:`List<java.util.List>`
| Gets the value of the annotation property. 
| This accessor method returns a reference to the live list, not a snapshot. Therefore any modification you make to the returned list will be present inside the JAXB object. This is why there is not a ``set`` method for the annotation property. 
| For example, to add a new item, do as follows: `getAnnotation().add(newItem);` 
| Objects of the following type(s) are allowed in the list `AnnotationInstance`
|          returns :ref:`List<java.util.List>`



| **getField** () → :ref:`List<java.util.List>`
| Gets the value of the field property. 
| This accessor method returns a reference to the live list, not a snapshot. Therefore any modification you make to the returned list will be present inside the JAXB object. This is why there is not a ``set`` method for the field property. 
| For example, to add a new item, do as follows: `getField().add(newItem);` 
| Objects of the following type(s) are allowed in the list `Field`
|          returns :ref:`List<java.util.List>`



| **getName** () → :ref:`String<java.lang.String>`
| Gets the value of the name property.
|          returns :ref:`String<java.lang.String>`  | possible object is `String`



| **setName** (value)
| Sets the value of the name property.
|          :ref:`String<java.lang.String>` value


| **getQualified** () → :ref:`String<java.lang.String>`
| Gets the value of the qualified property.
|          returns :ref:`String<java.lang.String>`  | possible object is `String`



| **setQualified** (value)
| Sets the value of the qualified property.
|          :ref:`String<java.lang.String>` value


| **getScope** () → :ref:`String<java.lang.String>`
| Gets the value of the scope property.
|          returns :ref:`String<java.lang.String>`  | possible object is `String`



| **setScope** (value)
| Sets the value of the scope property.
|          :ref:`String<java.lang.String>` value


| **isIncluded** () → boolean
| Gets the value of the included property.
|          returns boolean  | possible object is `Boolean`



| **setIncluded** (value)
| Sets the value of the included property.
|          :ref:`Boolean<java.lang.Boolean>` value



..  _com.github.markusbernhardt.xmldoclet.xjc.Method:

=======================================================================
Method
=======================================================================

*extends:* :ref:`Object<java.lang.Object>` 

| 
| Java class for method complex type. 
| The following schema fragment specifies the expected content contained within this class. `&lt;complexType name="method"&gt; &lt;complexContent&gt; &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt; &lt;sequence&gt; &lt;element name="comment" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/&gt; &lt;element name="tag" type="{}tagInfo" maxOccurs="unbounded" minOccurs="0"/&gt; &lt;element name="parameter" type="{}methodParameter" maxOccurs="unbounded" minOccurs="0"/&gt; &lt;element name="return" type="{}typeInfo" minOccurs="0"/&gt; &lt;element name="exception" type="{}typeInfo" maxOccurs="unbounded" minOccurs="0"/&gt; &lt;element name="annotation" type="{}annotationInstance" maxOccurs="unbounded" minOccurs="0"/&gt; &lt;/sequence&gt; &lt;attribute name="name" type="{http://www.w3.org/2001/XMLSchema}string" /&gt; &lt;attribute name="signature" type="{http://www.w3.org/2001/XMLSchema}string" /&gt; &lt;attribute name="qualified" type="{http://www.w3.org/2001/XMLSchema}string" /&gt; &lt;attribute name="scope" type="{}scope" /&gt; &lt;attribute name="abstract" type="{http://www.w3.org/2001/XMLSchema}boolean" default="false" /&gt; &lt;attribute name="final" type="{http://www.w3.org/2001/XMLSchema}boolean" default="false" /&gt; &lt;attribute name="included" type="{http://www.w3.org/2001/XMLSchema}boolean" default="true" /&gt; &lt;attribute name="native" type="{http://www.w3.org/2001/XMLSchema}boolean" default="false" /&gt; &lt;attribute name="synchronized" type="{http://www.w3.org/2001/XMLSchema}boolean" default="false" /&gt; &lt;attribute name="static" type="{http://www.w3.org/2001/XMLSchema}boolean" default="false" /&gt; &lt;attribute name="varArgs" type="{http://www.w3.org/2001/XMLSchema}boolean" default="false" /&gt; &lt;/restriction&gt; &lt;/complexContent&gt; &lt;/complexType&gt;`

| **Method** ()


| **getComment** () → :ref:`String<java.lang.String>`
| Gets the value of the comment property.
|          returns :ref:`String<java.lang.String>`  | possible object is `String`



| **setComment** (value)
| Sets the value of the comment property.
|          :ref:`String<java.lang.String>` value


| **getTag** () → :ref:`List<java.util.List>`
| Gets the value of the tag property. 
| This accessor method returns a reference to the live list, not a snapshot. Therefore any modification you make to the returned list will be present inside the JAXB object. This is why there is not a ``set`` method for the tag property. 
| For example, to add a new item, do as follows: `getTag().add(newItem);` 
| Objects of the following type(s) are allowed in the list `TagInfo`
|          returns :ref:`List<java.util.List>`



| **getParameter** () → :ref:`List<java.util.List>`
| Gets the value of the parameter property. 
| This accessor method returns a reference to the live list, not a snapshot. Therefore any modification you make to the returned list will be present inside the JAXB object. This is why there is not a ``set`` method for the parameter property. 
| For example, to add a new item, do as follows: `getParameter().add(newItem);` 
| Objects of the following type(s) are allowed in the list `MethodParameter`
|          returns :ref:`List<java.util.List>`



| **getReturn** () → :ref:`TypeInfo<com.github.markusbernhardt.xmldoclet.xjc.TypeInfo>`
| Gets the value of the return property.
|          returns :ref:`TypeInfo<com.github.markusbernhardt.xmldoclet.xjc.TypeInfo>`  | possible object is `TypeInfo`



| **setReturn** (value)
| Sets the value of the return property.
|          :ref:`TypeInfo<com.github.markusbernhardt.xmldoclet.xjc.TypeInfo>` value


| **getException** () → :ref:`List<java.util.List>`
| Gets the value of the exception property. 
| This accessor method returns a reference to the live list, not a snapshot. Therefore any modification you make to the returned list will be present inside the JAXB object. This is why there is not a ``set`` method for the exception property. 
| For example, to add a new item, do as follows: `getException().add(newItem);` 
| Objects of the following type(s) are allowed in the list `TypeInfo`
|          returns :ref:`List<java.util.List>`



| **getAnnotation** () → :ref:`List<java.util.List>`
| Gets the value of the annotation property. 
| This accessor method returns a reference to the live list, not a snapshot. Therefore any modification you make to the returned list will be present inside the JAXB object. This is why there is not a ``set`` method for the annotation property. 
| For example, to add a new item, do as follows: `getAnnotation().add(newItem);` 
| Objects of the following type(s) are allowed in the list `AnnotationInstance`
|          returns :ref:`List<java.util.List>`



| **getName** () → :ref:`String<java.lang.String>`
| Gets the value of the name property.
|          returns :ref:`String<java.lang.String>`  | possible object is `String`



| **setName** (value)
| Sets the value of the name property.
|          :ref:`String<java.lang.String>` value


| **getSignature** () → :ref:`String<java.lang.String>`
| Gets the value of the signature property.
|          returns :ref:`String<java.lang.String>`  | possible object is `String`



| **setSignature** (value)
| Sets the value of the signature property.
|          :ref:`String<java.lang.String>` value


| **getQualified** () → :ref:`String<java.lang.String>`
| Gets the value of the qualified property.
|          returns :ref:`String<java.lang.String>`  | possible object is `String`



| **setQualified** (value)
| Sets the value of the qualified property.
|          :ref:`String<java.lang.String>` value


| **getScope** () → :ref:`String<java.lang.String>`
| Gets the value of the scope property.
|          returns :ref:`String<java.lang.String>`  | possible object is `String`



| **setScope** (value)
| Sets the value of the scope property.
|          :ref:`String<java.lang.String>` value


| **isAbstract** () → boolean
| Gets the value of the abstract property.
|          returns boolean  | possible object is `Boolean`



| **setAbstract** (value)
| Sets the value of the abstract property.
|          :ref:`Boolean<java.lang.Boolean>` value


| **isFinal** () → boolean
| Gets the value of the final property.
|          returns boolean  | possible object is `Boolean`



| **setFinal** (value)
| Sets the value of the final property.
|          :ref:`Boolean<java.lang.Boolean>` value


| **isIncluded** () → boolean
| Gets the value of the included property.
|          returns boolean  | possible object is `Boolean`



| **setIncluded** (value)
| Sets the value of the included property.
|          :ref:`Boolean<java.lang.Boolean>` value


| **isNative** () → boolean
| Gets the value of the native property.
|          returns boolean  | possible object is `Boolean`



| **setNative** (value)
| Sets the value of the native property.
|          :ref:`Boolean<java.lang.Boolean>` value


| **isSynchronized** () → boolean
| Gets the value of the synchronized property.
|          returns boolean  | possible object is `Boolean`



| **setSynchronized** (value)
| Sets the value of the synchronized property.
|          :ref:`Boolean<java.lang.Boolean>` value


| **isStatic** () → boolean
| Gets the value of the static property.
|          returns boolean  | possible object is `Boolean`



| **setStatic** (value)
| Sets the value of the static property.
|          :ref:`Boolean<java.lang.Boolean>` value


| **isVarArgs** () → boolean
| Gets the value of the varArgs property.
|          returns boolean  | possible object is `Boolean`



| **setVarArgs** (value)
| Sets the value of the varArgs property.
|          :ref:`Boolean<java.lang.Boolean>` value



..  _com.github.markusbernhardt.xmldoclet.xjc.MethodParameter:

=======================================================================
MethodParameter
=======================================================================

*extends:* :ref:`Object<java.lang.Object>` 

| 
| Java class for methodParameter complex type. 
| The following schema fragment specifies the expected content contained within this class. `&lt;complexType name="methodParameter"&gt; &lt;complexContent&gt; &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt; &lt;sequence&gt; &lt;element name="type" type="{}typeInfo" minOccurs="0"/&gt; &lt;element name="annotation" type="{}annotationInstance" maxOccurs="unbounded" minOccurs="0"/&gt; &lt;/sequence&gt; &lt;attribute name="name" type="{http://www.w3.org/2001/XMLSchema}string" /&gt; &lt;/restriction&gt; &lt;/complexContent&gt; &lt;/complexType&gt;`

| **MethodParameter** ()


| **getType** () → :ref:`TypeInfo<com.github.markusbernhardt.xmldoclet.xjc.TypeInfo>`
| Gets the value of the type property.
|          returns :ref:`TypeInfo<com.github.markusbernhardt.xmldoclet.xjc.TypeInfo>`  | possible object is `TypeInfo`



| **setType** (value)
| Sets the value of the type property.
|          :ref:`TypeInfo<com.github.markusbernhardt.xmldoclet.xjc.TypeInfo>` value


| **getAnnotation** () → :ref:`List<java.util.List>`
| Gets the value of the annotation property. 
| This accessor method returns a reference to the live list, not a snapshot. Therefore any modification you make to the returned list will be present inside the JAXB object. This is why there is not a ``set`` method for the annotation property. 
| For example, to add a new item, do as follows: `getAnnotation().add(newItem);` 
| Objects of the following type(s) are allowed in the list `AnnotationInstance`
|          returns :ref:`List<java.util.List>`



| **getName** () → :ref:`String<java.lang.String>`
| Gets the value of the name property.
|          returns :ref:`String<java.lang.String>`  | possible object is `String`



| **setName** (value)
| Sets the value of the name property.
|          :ref:`String<java.lang.String>` value



..  _com.github.markusbernhardt.xmldoclet.xjc.ObjectFactory:

=======================================================================
ObjectFactory
=======================================================================

*extends:* :ref:`Object<java.lang.Object>` 

| This object contains factory methods for each  Java content interface and Java element interface  generated in the com.github.markusbernhardt.xmldoclet.xjc package.  
| An ObjectFactory allows you to programatically  construct new instances of the Java representation  for XML content. The Java representation of XML  content can consist of schema derived interfaces  and classes representing the binding of schema  type definitions, element declarations and model  groups.  Factory methods for each of these are  provided in this class.

| **ObjectFactory** ()
| Create a new ObjectFactory that can be used to create new instances of schema derived classes for package: com.github.markusbernhardt.xmldoclet.xjc


| **createRoot** () → :ref:`Root<com.github.markusbernhardt.xmldoclet.xjc.Root>`
| Create an instance of `Root`
|          returns :ref:`Root<com.github.markusbernhardt.xmldoclet.xjc.Root>`



| **createPackage** () → :ref:`Package<com.github.markusbernhardt.xmldoclet.xjc.Package>`
| Create an instance of `Package`
|          returns :ref:`Package<com.github.markusbernhardt.xmldoclet.xjc.Package>`



| **createAnnotation** () → :ref:`Annotation<com.github.markusbernhardt.xmldoclet.xjc.Annotation>`
| Create an instance of `Annotation`
|          returns :ref:`Annotation<com.github.markusbernhardt.xmldoclet.xjc.Annotation>`



| **createAnnotationElement** () → :ref:`AnnotationElement<com.github.markusbernhardt.xmldoclet.xjc.AnnotationElement>`
| Create an instance of `AnnotationElement`
|          returns :ref:`AnnotationElement<com.github.markusbernhardt.xmldoclet.xjc.AnnotationElement>`



| **createAnnotationInstance** () → :ref:`AnnotationInstance<com.github.markusbernhardt.xmldoclet.xjc.AnnotationInstance>`
| Create an instance of `AnnotationInstance`
|          returns :ref:`AnnotationInstance<com.github.markusbernhardt.xmldoclet.xjc.AnnotationInstance>`



| **createAnnotationArgument** () → :ref:`AnnotationArgument<com.github.markusbernhardt.xmldoclet.xjc.AnnotationArgument>`
| Create an instance of `AnnotationArgument`
|          returns :ref:`AnnotationArgument<com.github.markusbernhardt.xmldoclet.xjc.AnnotationArgument>`



| **createEnum** () → :ref:`Enum<com.github.markusbernhardt.xmldoclet.xjc.Enum>`
| Create an instance of `Enum`
|          returns :ref:`Enum<com.github.markusbernhardt.xmldoclet.xjc.Enum>`



| **createEnumConstant** () → :ref:`EnumConstant<com.github.markusbernhardt.xmldoclet.xjc.EnumConstant>`
| Create an instance of `EnumConstant`
|          returns :ref:`EnumConstant<com.github.markusbernhardt.xmldoclet.xjc.EnumConstant>`



| **createInterface** () → :ref:`Interface<com.github.markusbernhardt.xmldoclet.xjc.Interface>`
| Create an instance of `Interface`
|          returns :ref:`Interface<com.github.markusbernhardt.xmldoclet.xjc.Interface>`



| **createClass** () → :ref:`Class<com.github.markusbernhardt.xmldoclet.xjc.Class>`
| Create an instance of `Class`
|          returns :ref:`Class<com.github.markusbernhardt.xmldoclet.xjc.Class>`



| **createConstructor** () → :ref:`Constructor<com.github.markusbernhardt.xmldoclet.xjc.Constructor>`
| Create an instance of `Constructor`
|          returns :ref:`Constructor<com.github.markusbernhardt.xmldoclet.xjc.Constructor>`



| **createMethod** () → :ref:`Method<com.github.markusbernhardt.xmldoclet.xjc.Method>`
| Create an instance of `Method`
|          returns :ref:`Method<com.github.markusbernhardt.xmldoclet.xjc.Method>`



| **createMethodParameter** () → :ref:`MethodParameter<com.github.markusbernhardt.xmldoclet.xjc.MethodParameter>`
| Create an instance of `MethodParameter`
|          returns :ref:`MethodParameter<com.github.markusbernhardt.xmldoclet.xjc.MethodParameter>`



| **createField** () → :ref:`Field<com.github.markusbernhardt.xmldoclet.xjc.Field>`
| Create an instance of `Field`
|          returns :ref:`Field<com.github.markusbernhardt.xmldoclet.xjc.Field>`



| **createTypeInfo** () → :ref:`TypeInfo<com.github.markusbernhardt.xmldoclet.xjc.TypeInfo>`
| Create an instance of `TypeInfo`
|          returns :ref:`TypeInfo<com.github.markusbernhardt.xmldoclet.xjc.TypeInfo>`



| **createTypeParameter** () → :ref:`TypeParameter<com.github.markusbernhardt.xmldoclet.xjc.TypeParameter>`
| Create an instance of `TypeParameter`
|          returns :ref:`TypeParameter<com.github.markusbernhardt.xmldoclet.xjc.TypeParameter>`



| **createWildcard** () → :ref:`Wildcard<com.github.markusbernhardt.xmldoclet.xjc.Wildcard>`
| Create an instance of `Wildcard`
|          returns :ref:`Wildcard<com.github.markusbernhardt.xmldoclet.xjc.Wildcard>`



| **createTagInfo** () → :ref:`TagInfo<com.github.markusbernhardt.xmldoclet.xjc.TagInfo>`
| Create an instance of `TagInfo`
|          returns :ref:`TagInfo<com.github.markusbernhardt.xmldoclet.xjc.TagInfo>`




..  _com.github.markusbernhardt.xmldoclet.xjc.Package:

=======================================================================
Package
=======================================================================

*extends:* :ref:`Object<java.lang.Object>` 

| 
| Java class for package complex type. 
| The following schema fragment specifies the expected content contained within this class. `&lt;complexType name="package"&gt; &lt;complexContent&gt; &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt; &lt;sequence&gt; &lt;element name="comment" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/&gt; &lt;element name="tag" type="{}tagInfo" maxOccurs="unbounded" minOccurs="0"/&gt; &lt;element name="annotation" type="{}annotation" maxOccurs="unbounded" minOccurs="0"/&gt; &lt;element name="enum" type="{}enum" maxOccurs="unbounded" minOccurs="0"/&gt; &lt;element name="interface" type="{}interface" maxOccurs="unbounded" minOccurs="0"/&gt; &lt;element name="class" type="{}class" maxOccurs="unbounded" minOccurs="0"/&gt; &lt;/sequence&gt; &lt;attribute name="name" type="{http://www.w3.org/2001/XMLSchema}string" /&gt; &lt;/restriction&gt; &lt;/complexContent&gt; &lt;/complexType&gt;`

| **Package** ()


| **getComment** () → :ref:`String<java.lang.String>`
| Gets the value of the comment property.
|          returns :ref:`String<java.lang.String>`  | possible object is `String`



| **setComment** (value)
| Sets the value of the comment property.
|          :ref:`String<java.lang.String>` value


| **getTag** () → :ref:`List<java.util.List>`
| Gets the value of the tag property. 
| This accessor method returns a reference to the live list, not a snapshot. Therefore any modification you make to the returned list will be present inside the JAXB object. This is why there is not a ``set`` method for the tag property. 
| For example, to add a new item, do as follows: `getTag().add(newItem);` 
| Objects of the following type(s) are allowed in the list `TagInfo`
|          returns :ref:`List<java.util.List>`



| **getAnnotation** () → :ref:`List<java.util.List>`
| Gets the value of the annotation property. 
| This accessor method returns a reference to the live list, not a snapshot. Therefore any modification you make to the returned list will be present inside the JAXB object. This is why there is not a ``set`` method for the annotation property. 
| For example, to add a new item, do as follows: `getAnnotation().add(newItem);` 
| Objects of the following type(s) are allowed in the list `Annotation`
|          returns :ref:`List<java.util.List>`



| **getEnum** () → :ref:`List<java.util.List>`
| Gets the value of the enum property. 
| This accessor method returns a reference to the live list, not a snapshot. Therefore any modification you make to the returned list will be present inside the JAXB object. This is why there is not a ``set`` method for the enum property. 
| For example, to add a new item, do as follows: `getEnum().add(newItem);` 
| Objects of the following type(s) are allowed in the list `Enum`
|          returns :ref:`List<java.util.List>`



| **getInterface** () → :ref:`List<java.util.List>`
| Gets the value of the interface property. 
| This accessor method returns a reference to the live list, not a snapshot. Therefore any modification you make to the returned list will be present inside the JAXB object. This is why there is not a ``set`` method for the interface property. 
| For example, to add a new item, do as follows: `getInterface().add(newItem);` 
| Objects of the following type(s) are allowed in the list `Interface`
|          returns :ref:`List<java.util.List>`



| **getClazz** () → :ref:`List<java.util.List>`
| Gets the value of the clazz property. 
| This accessor method returns a reference to the live list, not a snapshot. Therefore any modification you make to the returned list will be present inside the JAXB object. This is why there is not a ``set`` method for the clazz property. 
| For example, to add a new item, do as follows: `getClazz().add(newItem);` 
| Objects of the following type(s) are allowed in the list `Class`
|          returns :ref:`List<java.util.List>`



| **getName** () → :ref:`String<java.lang.String>`
| Gets the value of the name property.
|          returns :ref:`String<java.lang.String>`  | possible object is `String`



| **setName** (value)
| Sets the value of the name property.
|          :ref:`String<java.lang.String>` value



..  _com.github.markusbernhardt.xmldoclet.xjc.Root:

=======================================================================
Root
=======================================================================

*extends:* :ref:`Object<java.lang.Object>` 

| 
| Java class for anonymous complex type. 
| The following schema fragment specifies the expected content contained within this class. `&lt;complexType&gt; &lt;complexContent&gt; &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt; &lt;sequence&gt; &lt;element name="package" type="{}package" maxOccurs="unbounded" minOccurs="0"/&gt; &lt;/sequence&gt; &lt;/restriction&gt; &lt;/complexContent&gt; &lt;/complexType&gt;`

| **Root** ()


| **getPackage** () → :ref:`List<java.util.List>`
| Gets the value of the package property. 
| This accessor method returns a reference to the live list, not a snapshot. Therefore any modification you make to the returned list will be present inside the JAXB object. This is why there is not a ``set`` method for the package property. 
| For example, to add a new item, do as follows: `getPackage().add(newItem);` 
| Objects of the following type(s) are allowed in the list `Package`
|          returns :ref:`List<java.util.List>`




..  _com.github.markusbernhardt.xmldoclet.xjc.TagInfo:

=======================================================================
TagInfo
=======================================================================

*extends:* :ref:`Object<java.lang.Object>` 

| 
| Java class for tagInfo complex type. 
| The following schema fragment specifies the expected content contained within this class. `&lt;complexType name="tagInfo"&gt; &lt;complexContent&gt; &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt; &lt;attribute name="name" type="{http://www.w3.org/2001/XMLSchema}string" /&gt; &lt;attribute name="text" type="{http://www.w3.org/2001/XMLSchema}string" /&gt; &lt;/restriction&gt; &lt;/complexContent&gt; &lt;/complexType&gt;`

| **TagInfo** ()


| **getName** () → :ref:`String<java.lang.String>`
| Gets the value of the name property.
|          returns :ref:`String<java.lang.String>`  | possible object is `String`



| **setName** (value)
| Sets the value of the name property.
|          :ref:`String<java.lang.String>` value


| **getText** () → :ref:`String<java.lang.String>`
| Gets the value of the text property.
|          returns :ref:`String<java.lang.String>`  | possible object is `String`



| **setText** (value)
| Sets the value of the text property.
|          :ref:`String<java.lang.String>` value



..  _com.github.markusbernhardt.xmldoclet.xjc.TypeInfo:

=======================================================================
TypeInfo
=======================================================================

*extends:* :ref:`Object<java.lang.Object>` 

| 
| Java class for typeInfo complex type. 
| The following schema fragment specifies the expected content contained within this class. `&lt;complexType name="typeInfo"&gt; &lt;complexContent&gt; &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt; &lt;sequence&gt; &lt;element name="wildcard" type="{}wildcard" minOccurs="0"/&gt; &lt;element name="generic" type="{}typeInfo" maxOccurs="unbounded" minOccurs="0"/&gt; &lt;/sequence&gt; &lt;attribute name="qualified" type="{http://www.w3.org/2001/XMLSchema}string" /&gt; &lt;attribute name="dimension" type="{http://www.w3.org/2001/XMLSchema}string" /&gt; &lt;/restriction&gt; &lt;/complexContent&gt; &lt;/complexType&gt;`

| **TypeInfo** ()


| **getWildcard** () → :ref:`Wildcard<com.github.markusbernhardt.xmldoclet.xjc.Wildcard>`
| Gets the value of the wildcard property.
|          returns :ref:`Wildcard<com.github.markusbernhardt.xmldoclet.xjc.Wildcard>`  | possible object is `Wildcard`



| **setWildcard** (value)
| Sets the value of the wildcard property.
|          :ref:`Wildcard<com.github.markusbernhardt.xmldoclet.xjc.Wildcard>` value


| **getGeneric** () → :ref:`List<java.util.List>`
| Gets the value of the generic property. 
| This accessor method returns a reference to the live list, not a snapshot. Therefore any modification you make to the returned list will be present inside the JAXB object. This is why there is not a ``set`` method for the generic property. 
| For example, to add a new item, do as follows: `getGeneric().add(newItem);` 
| Objects of the following type(s) are allowed in the list `TypeInfo`
|          returns :ref:`List<java.util.List>`



| **getQualified** () → :ref:`String<java.lang.String>`
| Gets the value of the qualified property.
|          returns :ref:`String<java.lang.String>`  | possible object is `String`



| **setQualified** (value)
| Sets the value of the qualified property.
|          :ref:`String<java.lang.String>` value


| **getDimension** () → :ref:`String<java.lang.String>`
| Gets the value of the dimension property.
|          returns :ref:`String<java.lang.String>`  | possible object is `String`



| **setDimension** (value)
| Sets the value of the dimension property.
|          :ref:`String<java.lang.String>` value



..  _com.github.markusbernhardt.xmldoclet.xjc.TypeParameter:

=======================================================================
TypeParameter
=======================================================================

*extends:* :ref:`Object<java.lang.Object>` 

| 
| Java class for typeParameter complex type. 
| The following schema fragment specifies the expected content contained within this class. `&lt;complexType name="typeParameter"&gt; &lt;complexContent&gt; &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt; &lt;sequence&gt; &lt;element name="bound" type="{http://www.w3.org/2001/XMLSchema}string" maxOccurs="unbounded" minOccurs="0"/&gt; &lt;/sequence&gt; &lt;attribute name="name" type="{http://www.w3.org/2001/XMLSchema}string" /&gt; &lt;/restriction&gt; &lt;/complexContent&gt; &lt;/complexType&gt;`

| **TypeParameter** ()


| **getBound** () → :ref:`List<java.util.List>`
| Gets the value of the bound property. 
| This accessor method returns a reference to the live list, not a snapshot. Therefore any modification you make to the returned list will be present inside the JAXB object. This is why there is not a ``set`` method for the bound property. 
| For example, to add a new item, do as follows: `getBound().add(newItem);` 
| Objects of the following type(s) are allowed in the list `String`
|          returns :ref:`List<java.util.List>`



| **getName** () → :ref:`String<java.lang.String>`
| Gets the value of the name property.
|          returns :ref:`String<java.lang.String>`  | possible object is `String`



| **setName** (value)
| Sets the value of the name property.
|          :ref:`String<java.lang.String>` value



..  _com.github.markusbernhardt.xmldoclet.xjc.Wildcard:

=======================================================================
Wildcard
=======================================================================

*extends:* :ref:`Object<java.lang.Object>` 

| 
| Java class for wildcard complex type. 
| The following schema fragment specifies the expected content contained within this class. `&lt;complexType name="wildcard"&gt; &lt;complexContent&gt; &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt; &lt;sequence&gt; &lt;element name="extendsBound" type="{}typeInfo" maxOccurs="unbounded" minOccurs="0"/&gt; &lt;element name="superBound" type="{}typeInfo" maxOccurs="unbounded" minOccurs="0"/&gt; &lt;/sequence&gt; &lt;/restriction&gt; &lt;/complexContent&gt; &lt;/complexType&gt;`

| **Wildcard** ()


| **getExtendsBound** () → :ref:`List<java.util.List>`
| Gets the value of the extendsBound property. 
| This accessor method returns a reference to the live list, not a snapshot. Therefore any modification you make to the returned list will be present inside the JAXB object. This is why there is not a ``set`` method for the extendsBound property. 
| For example, to add a new item, do as follows: `getExtendsBound().add(newItem);` 
| Objects of the following type(s) are allowed in the list `TypeInfo`
|          returns :ref:`List<java.util.List>`



| **getSuperBound** () → :ref:`List<java.util.List>`
| Gets the value of the superBound property. 
| This accessor method returns a reference to the live list, not a snapshot. Therefore any modification you make to the returned list will be present inside the JAXB object. This is why there is not a ``set`` method for the superBound property. 
| For example, to add a new item, do as follows: `getSuperBound().add(newItem);` 
| Objects of the following type(s) are allowed in the list `TypeInfo`
|          returns :ref:`List<java.util.List>`



