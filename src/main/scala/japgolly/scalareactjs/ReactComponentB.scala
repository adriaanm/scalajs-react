package japgolly.scalareactjs

import scala.scalajs.js._

final class ReactComponentB[Props](name: String) {

  def getInitialState[State](f: Props => State) = new B2[State](f)
  def initialState[State](s: => State) = getInitialState(_ => s)
  def stateless = initialState(())
  def render(render: Props => VDom) = stateless.render((p,_) => render(p))

  class B2[State] private[ReactComponentB](getInitialState: Props => State) {
    type ScopeB = ComponentScopeB[Props, State]

    def backend[Backend](f: ScopeB => Backend) = new B3[Backend](f)
    def noBackend = new B3[Unit](_ => ())
    def render(render: (Props, State) => VDom) = noBackend.render((p,s,_) => render(p,s))

    class B3[Backend] private[ReactComponentB](backend: ScopeB => Backend) {
      type ScopeU = ComponentScopeU[Props, State, Backend]
      type ScopeM = ComponentScopeM[Props, State, Backend]
      type ScopeWU = ComponentScopeWU[Props, State, Backend]

      def render(render: (Props, State, Backend) => VDom) =
        B4(s => render(s.props, s.state, s.backend)
          , undefined, undefined, undefined, undefined, undefined, undefined, undefined, undefined)

      case class B4 private[ReactComponentB](
          __render: ScopeU => VDom
          , getDefaultProps: UndefOr[() => Props]
          , componentWillMount: UndefOr[ScopeU => Unit]
          , componentDidMount: UndefOr[ScopeM => Unit]
          , componentWillUnmount: UndefOr[ScopeM => Unit]
          , componentWillUpdate: UndefOr[(ScopeWU, Props, State) => Unit]
          , componentDidUpdate: UndefOr[(ScopeM, Props, State) => Unit]
          , componentWillReceiveProps: UndefOr[(ScopeM, Props) => Unit]
          , shouldComponentUpdate: UndefOr[(ScopeM, Props, State) => Boolean]
          ) {

        def getDefaultProps(f: => Props): B4 = copy(getDefaultProps = () => f)
        def componentWillMount(f: ScopeU => Unit): B4 = copy(componentWillMount = f)
        def componentDidMount(f: ScopeM => Unit): B4 = copy(componentDidMount = f)
        def componentWillUnmount(f: ScopeM => Unit): B4 = copy(componentWillUnmount = f)
        def componentWillUpdate(f: (ScopeWU, Props, State) => Unit): B4 = copy(componentWillUpdate = f)
        def componentDidUpdate(f: (ScopeM, Props, State) => Unit): B4 = copy(componentDidUpdate = f)
        def componentWillReceiveProps(f: (ScopeM, Props) => Unit): B4 = copy(componentWillReceiveProps = f)
        def shouldComponentUpdate(f: (ScopeM, Props, State) => Boolean): B4 = copy(shouldComponentUpdate = f)

        def buildSpec = {
          @inline def set(o: Object, k: String, v: Any): Unit = o.asInstanceOf[Dynamic].updateDynamic(k)(v) // TODO share

          val spec = Dynamic.literal(
              "displayName" -> name,
              "_backend" -> 0,
              "render" -> (__render: ThisFunction)
            ).asInstanceOf[Object]

          @inline def setFnPS[T, R](fn: UndefOr[(T, Props, State) => R], name: String): Unit =
            fn.foreach { f =>
              val g = (t: T, p: WrapObj[Props], s: WrapObj[State]) => f(t, p.v, s.v)
              set(spec, name, g: ThisFunction)
            }

          val componentWillMount2 = (t: ScopeU) => {
            val scopeB = t.asInstanceOf[ScopeB]
            t.asInstanceOf[Dynamic].updateDynamic("_backend")(WrapObj(backend(scopeB)))
            componentWillMount.foreach(g => g(t))
          }
          set(spec, "componentWillMount", componentWillMount2: ThisFunction)

          val initStateFn: ScopeU => WrapObj[State] = scope => WrapObj(getInitialState(scope.props))
          set(spec, "getInitialState", initStateFn: ThisFunction)

          getDefaultProps.foreach(f => set(spec, "getDefaultProps", f: Function))
          componentWillUnmount.foreach(f => set(spec, "componentWillUnmount", f: ThisFunction))
          componentDidMount.foreach(f => set(spec, "componentDidMount", f: ThisFunction))

          setFnPS(componentWillUpdate, "componentWillUpdate")
          setFnPS(componentDidUpdate, "componentDidUpdate")
          setFnPS(shouldComponentUpdate, "shouldComponentUpdate")

          componentWillReceiveProps.foreach { f =>
            val g = (t: ScopeM, p: WrapObj[Props]) => f(t, p.v)
            set(spec, "componentWillReceiveProps", g: ThisFunction)
          }

          spec.asInstanceOf[ComponentSpec[Props]]
        }

        def create = new WComponentConstructor(React.createClass(buildSpec))
      }
    }
  }
}

object ReactComponentB {
  def apply[Props](name: String) = new ReactComponentB[Props](name)
}