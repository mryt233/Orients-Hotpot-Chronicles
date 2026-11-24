"""从真实 Java 几何导出查看器破坏粒子采样；不启动游戏或创建窗口。作者：mryt233"""
from pathlib import Path
import json
import os
import subprocess
from project_env import jdk21

ROOT = Path(__file__).resolve().parent.parent
OUT = ROOT / 'build/part-break-preview'


def main():
    OUT.mkdir(parents=True, exist_ok=True)
    classes = OUT / 'classes'
    classes.mkdir(exist_ok=True)
    # 由工程自身提供公共测试类路径，避免误选平台注入过的缓存映射或重编译依赖源码。
    init = OUT / 'classpath.init.gradle'
    classpath = OUT / 'classpath.txt'
    init.write_text('''// 作者：mryt233
    gradle.projectsEvaluated {
        def p = gradle.rootProject.project(':common')
        p.tasks.register('writeBreakPreviewClasspath') {
            dependsOn p.tasks.named('testClasses')
            doLast { new File(gradle.rootProject.projectDir, 'build/part-break-preview/classpath.txt').text = p.sourceSets.test.runtimeClasspath.asPath }
        }
    }
    ''', encoding='utf-8')
    env = dict(os.environ, JAVA_HOME=str(jdk21()))
    subprocess.run([str(ROOT / 'gradlew.bat'), '--offline', '--no-daemon', '--console=plain',
                    '-I', str(init), ':common:writeBreakPreviewClasspath'], cwd=ROOT, env=env, check=True)
    cp = classpath.read_text().replace('\\', '/')
    quote = lambda value: json.dumps(str(value).replace('\\', '/'))
    args = OUT / 'compile.args'
    args.write_text('-proc:none\n-sourcepath ""\n-encoding UTF-8\n-classpath ' + quote(cp) + '\n-d ' + quote(classes)
                    + '\n' + quote(ROOT / 'tools/java/PotBreakPreviewData.java') + '\n', encoding='utf-8')
    subprocess.run([str(jdk21() / 'bin/javac.exe'), '-J-Duser.language=en', '@' + str(args)], cwd=ROOT, check=True)
    dest = ROOT / 'viewer/pot_break_samples.json'
    run_args = OUT / 'run.args'
    run_args.write_text('-Djava.awt.headless=true\n-classpath ' + quote(str(classes) + ';' + cp)
                        + '\nPotBreakPreviewData\n' + quote(dest) + '\n', encoding='utf-8')
    subprocess.run([str(jdk21() / 'bin/java.exe'), '@' + str(run_args)], cwd=ROOT, check=True)
    data = json.loads(dest.read_text())
    print('真实粒子采样已导出：', len(data['x']), len(data['z']), '高灶抬高', data['tall_lift'])


if __name__ == '__main__':
    main()
