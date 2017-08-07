package caustic.syntax.ops

/**
 * Created by ashwinmadavan on 8/10/17.
 */
class ObjectOps {

  //  // Object Operations.
  //  implicit class ObjectOps(x: Object) extends Dynamic {
  //
  //    def +=(y: Transaction)(implicit ctx: Context): Unit = ctx += write(x.key, read(x.key) + y)
  //    def -=(y: Transaction)(implicit ctx: Context): Unit = ctx += write(x.key, read(x.key) - y)
  //    def *=(y: Transaction)(implicit ctx: Context): Unit = ctx += write(x.key, read(x.key) * y)
  //    def /=(y: Transaction)(implicit ctx: Context): Unit = ctx += write(x.key, read(x.key) / y)
  //    def %=(y: Transaction)(implicit ctx: Context): Unit = ctx += write(x.key, read(x.key) % y)
  //    def ++=(y: Transaction)(implicit ctx: Context): Unit = ctx += write(x.key, read(x.key) ++ y)
  //    def exists: Transaction = x.isEmpty || x.get("$fields").isEmpty
  //
  //    def selectDynamic(name: String): Object = this.x.owner match {
  //      case Some(_) => this.x.deref.get(name)
  //      case None => this.x.get(name)
  //    }
  //
  //    def updateDynamic(name: String)(value: Transaction): Unit = this.x.owner match {
  //      case Some(_) => this.x.deref.set(name, value)
  //      case None => this.x.set(name, value)
  //    }
  //
  //    def applyDynamic(name: String)(args: Transaction*): Object =
  //      args.foldLeft(this.x)((a, b) => a.get(b))
  //
  //    def apply(name: Transaction): Object =
  //      this.x.get(name)
  //
  //    def update(name: Transaction, value: Transaction): Unit =
  //      this.x.set(name, value)
  //
  //  }

}
