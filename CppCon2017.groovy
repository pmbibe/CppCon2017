
stage("Fire deps"){
    overrides = readFile file: "overrides.txt"
    String command = "info . --build_order=${overrides} --json bo.js"
    client.run(command: command)
    def bo_json = readJSON file: "./bo.js"
    def build_order = bo_json["groups"]
    for (int i=1; i< build_order.size();i++){
        tasks = [:]
        for(int j=0; j<build_order[i].size();j++){
            def package_reference = build_order[i][j]
            tasks[package_reference] = {build(job: "SimpleBuild",
                parameters: [string(name:"package_reference",
                    value: package_reference),
                    string(name: "overrides",value:
                        overrides)])
            }
        }
        parallel(tasks)
    }
}
properties([parameters([string(name: 'package_reference'),
                        string(name: 'overrides') ])])
stage("Create Conanfile"){
    template = """from conans import ConanFile
        class TestConan(ConanFile):
            requires = "${package_referrence}",${overrides_text}"""
    writeFile file:'conanfile.py', text:template
}
stage("Install package"){
    String cmd = "install . -r ${servername} --build=missing"
    client.run(command: cmd)
}
stage("Upload packages"){
    String cmd = "upload ${package_reference} --all -r ${servername}"
    client.run(command: cmd)
}