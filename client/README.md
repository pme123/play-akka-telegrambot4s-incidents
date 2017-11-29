# Scala.js & Binding.scala Client
Once again it's build with [Binding.scala](https://github.com/ThoughtWorksInc/Binding.scala)

I want to point out here some interesting points, when working with Scala.js and Binding.scala.

**This is work in progress.**

# Play / Scala.js
## Versioned Assets
To use the versioned Assets provided by Play you need [ScalaJS Routes](https://github.com/pme123/play-wsocket-scalajs#scalajs-routes).

# Binding.scala
## Close to pure HTML
Binding.scala uses [scala-xml](https://github.com/scala/scala-xml) 
the standard Scala XML library. This allows to have code like:
```scala
<div class="ui segment">
  <p></p>
</div>
```
As you can see this is already valid HTML code. 
So all you need to do:

* Copy the HTML from your Designers and add the dynamic parts, like:
```scala
<div class="ui segment">
  <p>{myDynamicContent}</p>
</div>
```
### Restrictions
There are 2 things you need to be aware of:

1. The HTML must be valid XML. So for example each tag must be closed:
    ```scala
    <img src={asset.path}> // throws compile exception: in XML literal: expected closing tag of img
    <img src={asset.path}/> // is ok
    <img src={asset.path}></img> // is ok
    ```
    And yes it fails when compiling with a nice message - so you get **compile-safe HTML;)**.
2. scala-xml only accepts Strings as parameters:
    ```scala
    <select disabled="true"></select>
    ```
    throws:
    ```
         type mismatch;
                [error]  found   : String("true")
                [error]  required: Boolean
                [error]       <select disabled="true"
    ```
    To fix that, add `curly braces`:
    ```scala
    <select disabled={true}></select>
    ```
    This allows you to add any code (dynamic part). 
    And still if you put something that is not correct you get a compile exception.
    
### Convenient component reference
With version 11 Binding.scala allows you directly to refer to the component by its id:

```scala
<select id="levelFilterSelect"
        onchange={_: Event =>
                changeFilterLevel(s"${levelFilterSelect.value}")}>
        {...}
</select>
```
That makes the code pretty clean. 
The only downside is that Intellij will indicate a compile problem.

## Composition
Binding.scala allows you to compose your code pretty easily. It's more or less decomposing
a common function or class.

### Example
Here is a simple example. We have:
```scala
  @dom
  private def coloredSegments() =
    <div class="ui segments">
      <div class="ui segment">
        <p>Top</p>
      </div>
      <div class="ui red segment">
        <p>Middle</p>
      </div>
      <div class="ui blue segment">
        <p>Middle</p>
      </div>
      <div class="ui green segment">
        <p>Middle</p>
      </div>
      <div class="ui yellow segment">
        <p>Bottom</p>
      </div>
    </div>
```
This is a common function in Binding.scala. As you can see the @dom makes the difference.
(For simplicity there is no actual binding).

So lets reduce the duplications a bit:
```scala
 @dom
  def coloredSegments() =
    <div class="ui segments">
      {coloredSegment("Top").bind}{//
      coloredSegment("Middle", "red").bind}{//
      coloredSegment("Middle", "blue").bind}{//
      coloredSegment("Middle", "green").bind}{//
      coloredSegment("Bottom", "yellow").bind}
    </div>
    
  @dom
  def coloredSegment(name:String, color: String = "") =
    <div class={s"ui $color segment"}>
       <p>{name}</p>
    </div>
```
Easy right? 

## Data Binding
As the name indicates it is all about data binding. 
We have 2 variations:

1. `Vars[T]` is a sequence of any type.
2. `Var[T]` is one of any type.

You can do 3 different things:

1. Bind the actual value (read):
    ```scala
    val incidents = uiState.incidents.bind
    val level = uiState.filterLevel.bind
    incidents.filter(in => in.level.filter(level))
    ```
   Whenever `incidents` or `level` is updated the `filter` function is reevaluated.
2. Get the actual value: `myVar.value` or `myVars.value`.
3. Update the actual value:
    * For single values it is a simple assignment: `myVar.value = 12`
    * For a sequence you have manipulation functions:
    ```scala
    myVars.value.clear() // clears the sequence -> empty sequence
    myVars.value -= v // removes v from the sequence
    myVars.value += v // adds v from the sequence
    myVars.insert(0, v) // inserts v at index 0 to the sequence
    ...
    ```

## The harder Parts
Binding.scala is pretty easy to learn, especially compared to usual web-frameworks like React.js or Angular.

The following are the things I had - resp. still have some pain:

### Handling of Options and Collections
This is the hardest. Here is a step-by-step example: 
[Binding.scala-Google](https://github.com/pme123/Binding.scala-Google-Maps#dive-a-bit-deeper)

And here an explanation: 
[Stackoverflow](https://stackoverflow.com/questions/42498968/when-i-use-binding-scala-i-got-the-error-each-instructions-must-be-inside-a-sd/42498969#42498969)

 
If you have troubles understanding it, please check out [Binding.scala-Google-Maps](https://github.com/pme123/Binding.scala-Google-Maps), where I explained all the details.

