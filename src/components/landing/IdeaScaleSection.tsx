import Image from "next/image";
import { ScriptText } from "./ScriptText";

export function IdeaScaleSection() {
  return (
    <section className="relative overflow-hidden px-2 pb-16 sm:px-4">
      <p className="mb-2 text-[72px] leading-none text-[#464b45] sm:text-[118px]">
        IDEA
      </p>

      <div className="relative mx-auto aspect-[735/412] w-full max-w-[735px]">
        <Image
          src="/images/idea-scale.png"
          alt="Person reading by the sea"
          fill
          className="object-cover"
        />
        <ScriptText className="absolute left-[45%] top-[35%] text-[40px] text-white sm:text-[65px]">
          Thats mozaara
        </ScriptText>
        <p className="absolute bottom-4 right-4 text-[72px] leading-none text-white sm:text-[118px]">
          SCALE
        </p>
      </div>
    </section>
  );
}
