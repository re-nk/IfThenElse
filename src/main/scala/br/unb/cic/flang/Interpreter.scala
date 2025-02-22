package br.unb.cic.flang

import Declarations._
import StateMonad._

object Interpreter {

  /** This implementation relies on a state monad.
    *
    * Here we replace the substitution function (that needs to traverse the AST
    * twice during interpretation), by a 'global' state that contains the
    * current 'bindings'. The bindings are pairs from names to integers.
    *
    * We only update the state when we are interpreting a function application.
    * This implementation deals with sections 6.1 and 6.2 of the book
    * "Programming Languages: Application and Interpretation". However, here we
    * use a monad state, instead of passing the state explicitly as an agument
    * to the eval function.
    *
    * Sections 6.3 and 6.4 improves this implementation. We will left such an
    * improvements as an exercise.
    */
  def eval(expr: Expr, declarations: List[FDeclaration]): M[Integer] =
    expr match {
      case CInt(v) => pure(v)
      case Add(lhs, rhs) =>
        bind(eval(lhs, declarations))({ l =>
          bind(eval(rhs, declarations))({ r => pure(l + r) })
        })
      case Mul(lhs, rhs) =>
        bind(eval(lhs, declarations))({ l =>
          bind(eval(rhs, declarations))({ r => pure(l * r) })
        })
      case Id(name) => bind(get())({ state => pure(lookupVar(name, state)) })
      case App(name, arg) => {
        val fdecl = lookup(name, declarations)
        bind(eval(arg, declarations))({ value =>
          bind(get())({ s1 =>
            bind(put(declareVar(fdecl.arg, value, s1)))({ s2 =>
              eval(fdecl.body, declarations)
            })
          })
        })
      }
      // booleanos: 
      case CTrue() => pure(1) // True is any non 0 value
      case CFalse() => pure(0)
      // boolean operators
      case Not(e) => eval(e, declarations).map(b => if (b == 0) 1 else 0)
      
      case And(lhs, rhs) => for {
        l <- eval(lhs, declarations)
        r <- eval(rhs, declarations)
      } yield if (l != 0 && r != 0) 1 else 0

      case Or(lhs, rhs) => for {
        l <- eval(lhs, declarations)
        r <- eval(rhs, declarations)
      } yield if (l != 0 || r != 0) 1 else 0

      case CEquals(lhs, rhs) => for {
        l <- eval(lhs, declarations)
        r <- eval(rhs, declarations)
      } yield if (l == r) 1 else 0

      // condicional
      case IfThenElse(cond, ifTrue, ifFalse) => 
        bind(eval(cond, declarations))({ c =>
          if(c != 0) eval(ifTrue, declarations)
          else eval(ifFalse, declarations)
        })
    }
}
