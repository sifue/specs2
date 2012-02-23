package org.specs2
package reporter

import specification._
import main._
import io._
import org.scalacheck._
import execute.Success

class SelectionSpec extends Specification { def is =
                                                                                                                      """
 Before executing and reporting a specification, the fragments must be selected:

 * with the ex argument
 * with tags                                                                                                           """^
                                                                                                                       p ^
 "First of all examples are filtered"                                                                                  ^
   "when the user specifies a regular expression: ex = ex1.*"                                                          ^
     "in the spec"                                                                                                     ! filter().e1^
     "on the command line"                                                                                             ! filter().e2^
   "if no filter is specified, nothing must be filtered out"                                                           ! filter().e3^
                                                                                                                       p^
 "It is possible to select only some previously executed fragments"                                                    ^
   "wasIssue selects only the fragments which were failed or in error"                                                 ! rerun().e1^
                                                                                                                       end
  
  case class filter() extends WithSelection {
    def e1 = select(args(ex = "ex1") ^ ex1 ^ ex2).toString must not contain("ex2")
    def e2 = select(ex1 ^ ex2)(Arguments("ex", "ex1")).toString must not contain("ex2")
    def e3 = select(ex1 ^ ex2).toString must contain("ex1")
  }

  case class rerun() extends WithSelection {
    /**
     * The storing trait 'decides' to keep only the example 1 because of a previous run
     */
    override val selection = new DefaultSelection with DefaultSequence with DefaultStoring with MockOutput {
      override def includePrevious(specName: SpecName, e: Example, args: Arguments) = e.desc.toString == "e1"
    }

    def e1 = {
      val fragments: Fragments = wasIssue ^ sequential ^ example("e1") ^ step("s1") ^ example("e2")
      select(fragments).toString must_== "List(SpecStart(Object), Example(e1), Step, SpecEnd(Object))"
    }
  }

  val ex1 = "ex1" ! success
  val ex2 = "ex2" ! success

}

trait WithSelection extends FragmentsBuilder {
  val selection = new DefaultSelection with DefaultSequence with MockOutput

  def selectSequence(fs: Fragments): Seq[FragmentSeq] = {
    selection.sequence(fs.specName, selection.select(fs.arguments)(SpecificationStructure(fs)).is.fragments)(fs.arguments).toList
  }
  def select(f: Fragments)(implicit args: Arguments = Arguments()) = {
    val fs = new Specification { def is = f }.content
    selection.select(args)(SpecificationStructure(fs)).content.fragments.toList.map(_.toString)
  }
  def step(message: String) = Step({selection.println(message); reporter.println(message)})
  def example(message: String) = message ! { selection.println(message); reporter.println(message); Success() }
  val reporter = new DefaultReporter with Exporting with MockOutput {
    def export(implicit args: Arguments): ExecutingSpecification => ExecutedSpecification = (spec: ExecutingSpecification) => spec.executed
  }
}
